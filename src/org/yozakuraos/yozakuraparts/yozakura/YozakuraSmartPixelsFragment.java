/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.os.Bundle;

import androidx.preference.ListPreference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

/**
 * YozakuraOS cp102: expose the Smart Pixels settings page under the Yozakura
 * display tile. Smart Pixels itself was ported in cp98 (SystemUI core) and got a
 * QS tile + long-press dialog in cp100/cp101; this fragment adds the discoverable
 * Settings entry, mirroring InfinitySuite's SmartPixels fragment.
 *
 * The two controls are self-binding lineageos.preference widgets that write
 * straight into Settings.Secure.SMART_PIXEL_FILTER_ENABLED / _PERCENT, the exact
 * keys SmartPixelSettings observes, so no glue code is needed. The QS dialog keeps
 * the fine-grained slider; this page offers the master switch plus coarse presets.
 */
public class YozakuraSmartPixelsFragment extends SettingsPreferenceFragment {

    private static final String PREF_PERCENT = "smart_pixel_filter_percent";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_smart_pixels);
        getActivity().setTitle(R.string.smart_pixels_title);

        final ListPreference percent = findPreference(PREF_PERCENT);
        if (percent != null) {
            // Show the selected percentage as the summary without ListPreference
            // running String.format() over an entry that itself contains '%'.
            percent.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }
    }
}
