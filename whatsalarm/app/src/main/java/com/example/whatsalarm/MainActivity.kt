package com.example.whatsalarm

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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

        binding.keywords.setText(
            prefs.getString("keywords", "good morning,meeting,urgent,container")
        )

        binding.btnQuietStart.text = prefs.getString("quietStart", "22:00")
        binding.btnQuietEnd.text = prefs.getString("quietEnd", "07:00")

        // ----- TOGGLE ON/OFF -----
        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }

        // ----- SAVE KEYWORDS -----
        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()

            // Show Toast
            android.widget.Toast.makeText(this, "Keywords saved ✅", android.widget.Toast.LENGTH_SHORT).show()

            // Change button text temporarily
            binding.saveBtn.text = "Saved!"
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save" }, 1500)
        }

        // ----- QUIET HOURS PICKERS -----
        binding.btnQuietStart.setOnClickListener { pickTime("quietStart") }
        binding.btnQuietEnd.setOnClickListener { pickTime("quietEnd") }

        // ----- OPEN NOTIFICATION ACCESS -----
        binding.btnOpenNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun pickTime(key: String) {
        val cal = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, h, m ->
                val t = "%02d:%02d".format(h, m)
                prefs.edit().putString(key, t).apply()

                if (key == "quietStart") binding.btnQuietStart.text = t
                else binding.btnQuietEnd.text = t
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }
}
