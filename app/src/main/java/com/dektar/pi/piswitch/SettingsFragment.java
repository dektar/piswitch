package com.dektar.pi.piswitch;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Controls the settings.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
