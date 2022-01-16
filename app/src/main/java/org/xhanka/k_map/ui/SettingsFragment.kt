package org.xhanka.k_map.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.xhanka.k_map.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}