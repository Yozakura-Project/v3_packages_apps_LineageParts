/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

/**
 * YozakuraOS cp115: cutout progress ring settings page. The ring was ported in cp114
 * (SystemUI com.android.systemui.cutoutprogress), gated by Settings.Secure
 * cutout_progress_enabled (default off).
 *
 * Every control on this page is self-binding: the switches, lists and seek bars write
 * straight into android Settings.Secure using the keys CutoutProgressSettings reads, and
 * its ContentObserver picks the change up without a reboot. So this fragment only sets
 * the title and the list summaries.
 *
 * Infinity drives the list summaries with android:summary="%s"; we use the built-in
 * summary provider instead, which shows the selected entry without ListPreference running
 * String.format() over it (the Monet page hit UnknownFormatConversionException that way
 * on entries containing a literal '%').
 */
public class YozakuraCutoutProgressFragment extends SettingsPreferenceFragment {

    private static final String[] LIST_KEYS = {
        "cutout_progress_ring_color_mode",
        "cutout_progress_music_color_mode",
        "cutout_progress_easing",
        // cp116
        "cutout_progress_finish_style",
        "cutout_progress_percent_position",
        "cutout_progress_filename_position",
        "cutout_progress_filename_truncate",
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_cutout_progress);
        getActivity().setTitle(R.string.cutout_progress_title);

        for (String key : LIST_KEYS) {
            final Preference pref = findPreference(key);
            if (pref instanceof ListPreference) {
                pref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }
        }
    }
}
