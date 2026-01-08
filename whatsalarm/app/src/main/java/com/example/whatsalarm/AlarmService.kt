package com.example.whatsalarm

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.Activity
import android.content.Context
import android.net.Uri

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused = false
    private var lastKeyword: String? = null

    companion object {
        const val CHANNEL_ID = "whatsalarm_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action

        when (action) {
            "START_ALARM" -> {
                lastKeyword = intent.getStringExtra("keyword")
                startAlarm()
            }
            "STOP_ALARM" -> stopAlarm()
        }

        return START_STICKY
    }

    private fun startAlarm() {
        if (mediaPlayer != null) return  // Already playing

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val toneUri = prefs.getString("alarmTone", null)?.let { Uri.parse(it) }
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, toneUri)
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            start()
        }

        // Show popup alert with keyword
        lastKeyword?.let { keyword ->
            val popupIntent = Intent(this, AlarmPopupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmPopupActivity.EXTRA_KEYWORD, keyword)
            }
            startActivity(popupIntent)
        }


        // Optional: Show persistent notification
        createNotification()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPaused = false
        stopForeground(true)
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "WhatsAlarm Notifications"
            val descriptionText = "Notification channel for WhatsAlarm"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = descriptionText
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsAlarm is ringing")
            .setContentText(lastKeyword?.let { "Keyword: $it" } ?: "")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
