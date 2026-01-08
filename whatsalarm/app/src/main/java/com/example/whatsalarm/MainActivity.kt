package com.example.whatsalarm

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsalarm.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ----- INITIAL UI VALUES -----
        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)
        binding.keywords.setText(prefs.getString("keywords", "good morning,meeting,urgent,container"))
        binding.btnQuietStart.text = formatTimeForDisplay(prefs.getString("quietStart","22:00")!!)
        binding.btnQuietEnd.text = formatTimeForDisplay(prefs.getString("quietEnd","07:00")!!)

        // ----- TOGGLE ON/OFF -----
        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }

        // ----- SAVE KEYWORDS -----
        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()
            Toast.makeText(this,"Keywords saved ✅", Toast.LENGTH_SHORT).show()
            binding.saveBtn.text = "Saved!"
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save" },1500)
        }

        // ----- QUIET HOURS PICKERS -----
        binding.btnQuietStart.setOnClickListener { pickTime("quietStart") }
        binding.btnQuietEnd.setOnClickListener { pickTime("quietEnd") }

        // ----- OPEN NOTIFICATION ACCESS -----
        binding.btnOpenNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // ----- CHOOSE RINGTONE -----
        binding.btnChooseRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    prefs.getString("alarmTone", null)?.let { Uri.parse(it) })
            }
            startActivityForResult(intent, 101)
        }
    }

    private fun pickTime(key: String) {
        val cal = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, h, m ->
                val tDisplay = formatTimeForDisplay("%02d:%02d".format(h,m))
                prefs.edit().putString(key, "%02d:%02d".format(h,m)).apply()
                if (key == "quietStart") binding.btnQuietStart.text = tDisplay
                else binding.btnQuietEnd.text = tDisplay
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun formatTimeForDisplay(time24: String): String {
        val parts = time24.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%02d:%02d %s".format(h12, m, amPm)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                prefs.edit().putString("alarmTone", it.toString()).apply()
                Toast.makeText(this, "Ringtone updated ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
