package com.example.whatsalarm

import android.media.RingtoneManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("com.whatsapp")) return // only WhatsApp

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) return // master toggle

        val quietStart = prefs.getString("quietStart", "22:00")!!
        val quietEnd = prefs.getString("quietEnd", "07:00")!!
        if (inQuietHours(quietStart, quietEnd)) return // quiet hours

        // Get all possible text from notification
        val extras = sbn.notification.extras
        val texts = mutableListOf<String>()
        extras.getCharSequence("android.text")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.bigText")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.title")?.let { texts.add(it.toString().lowercase()) }

        if (texts.isEmpty()) return // nothing to match

        val keywords = prefs.getString("keywords", "")!!
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        // Check if any text line contains any keyword
        if (texts.any { text -> keywords.any { keyword -> text.contains(keyword) } }) {
            val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(applicationContext, tone)
            r.play()
        }
    }

    private fun inQuietHours(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        fun mins(s: String): Int {
            val p = s.split(":")
            return p[0].toInt() * 60 + p[1].toInt()
        }

        val s = mins(start)
        val e = mins(end)

        return if (s < e) (cur in s..e) else (cur >= s || cur <= e)
    }
}
