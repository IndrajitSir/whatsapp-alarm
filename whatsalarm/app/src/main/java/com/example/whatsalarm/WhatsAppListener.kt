package com.example.whatsalarm

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Calendar

class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // Only react to WhatsApp notifications
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("com.whatsapp")) return

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Master toggle
        if (!prefs.getBoolean("enabled", true)) return

        // Quiet hours check
        val quietStart = prefs.getString("quietStart", "22:00")!!
        val quietEnd   = prefs.getString("quietEnd", "07:00")!!
        if (inQuietHours(quietStart, quietEnd)) return

        // Pull *all* possible text from the notification
        val extras = sbn.notification.extras
        val texts = mutableListOf<String>()

        extras.getCharSequence("android.text")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.bigText")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.title")?.let { texts.add(it.toString().lowercase()) }

        if (texts.isEmpty()) return

        // Keywords
        val keywords = prefs.getString("keywords", "")!!
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (keywords.isEmpty()) return

        // Check for match and find the first keyword matched
        var matchedKeyword: String? = null
        loop@ for (text in texts) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    matchedKeyword = keyword
                    break@loop
                }
            }
        }

        // Trigger alarm service if a keyword matched
        matchedKeyword?.let {
            val intent = Intent(this, AlarmService::class.java)
            intent.action = "START_ALARM"
            intent.putExtra("keyword", it)  // pass matched keyword
            startService(intent)
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

        // Handles ranges that cross midnight
        return if (s < e) (cur in s..e) else (cur >= s || cur <= e)
    }
}
