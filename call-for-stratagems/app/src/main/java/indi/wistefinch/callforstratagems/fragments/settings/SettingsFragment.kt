package indi.wistefinch.callforstratagems.fragments.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import indi.wistefinch.callforstratagems.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}