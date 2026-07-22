/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.os.Bundle;

import androidx.preference.Preference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;
import org.yozakuraos.yozakuraparts.YozakuraUtils;

public class YozakuraLockScreenFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CLOCK_STYLE = "lock_screen_custom_clock_style";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_lockscreen);
        getActivity().setTitle(R.string.yozakura_lockscreen_category);

        final Preference clockStyle = findPreference(KEY_CLOCK_STYLE);
        if (clockStyle != null) {
            clockStyle.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_CLOCK_STYLE.equals(preference.getKey())) {
            // Let the value persist to Settings.Secure first, then restart
            // SystemUI so the new clock face applies immediately.
            YozakuraUtils.restartSystemUIDelayed();
        }
        return true;
    }
}
