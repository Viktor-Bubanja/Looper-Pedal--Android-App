package com.example.looper.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.looper.audio.AudioFilePlayer
import com.example.looper.R

class PreferencesActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs?.registerOnSharedPreferenceChangeListener(this)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.preferences,
                PreferencesFragment()
            )
            .commit()
    }


    override fun onStop() {
        super.onStop()
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "enableLooping" -> sharedPreferences?.getBoolean(key, true)
                ?.let { AudioFilePlayer.isLoopingFile = it }
        }
    }

    class PreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference, rootKey)
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, PreferencesActivity::class.java)
    }
}