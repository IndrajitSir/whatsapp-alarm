package com.example.whatsalarm

import android.media.RingtoneManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val pkg = sbn.packageName ?: return
        if (!pkg.contains("com.whatsapp")) return   // only WhatsApp

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // master toggle
        if (!prefs.getBoolean("enabled", true)) return

        // quiet hours
        val quietStart = prefs.getString("quietStart", "22:00")!!
        val quietEnd   = prefs.getString("quietEnd", "07:00")!!
        if (inQuietHours(quietStart, quietEnd)) return

        // keywords
        val text = sbn.notification.extras.getString("android.text")?.lowercase() ?: ""
        val list = prefs.getString("keywords", "")!!
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (list.any { text.contains(it) }) {
            val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(applicationContext, tone)
            r.play()
        }
    }

    private fun inQuietHours(start:String, end:String): Boolean {
        val now = Calendar.getInstance()
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        fun mins(s:String): Int {
            val p = s.split(":")
            return p[0].toInt()*60 + p[1].toInt()
        }

        val s = mins(start)
        val e = mins(end)

        return if (s < e) (cur in s..e) else (cur >= s || cur <= e)
    }
}
