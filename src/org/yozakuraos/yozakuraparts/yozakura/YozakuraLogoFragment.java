/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.util.Locale;

/**
 * YozakuraOS cp113: status bar logo settings page. The logo was ported in cp112
 * (SystemUI com.android.systemui.infinity.logo), gated by Settings.System
 * status_bar_logo (default off).
 *
 * The master switch, style, position and colour mode are self-binding widgets that
 * write straight into android Settings.System using the keys LogoImage observes, so
 * this fragment only drives the two things they cannot:
 *
 *  - the custom colour, because MonetColorPickerPreference is persistent="false" and
 *    emits an RRGGBB hex string, while LogoImage reads status_bar_logo_color_picker as
 *    an opaque ARGB int (its own default is 0xff1a73e8);
 *  - hiding that picker unless the colour mode is actually "custom", mirroring how the
 *    DynamicBar page gates its compact-notification switch.
 */
public class YozakuraLogoFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_COLOR_MODE = "status_bar_logo_color";
    private static final String KEY_CUSTOM_COLOR = "status_bar_logo_custom_color";
    private static final String KEY_STYLE = "status_bar_logo_style";
    private static final String KEY_POSITION = "status_bar_logo_position";

    /** Key LogoImage reads for the custom colour. */
    private static final String KEY_COLOR_PICKER = "status_bar_logo_color_picker";

    /** Value of status_bar_logo_color that means "use the custom colour". */
    private static final int COLOR_MODE_CUSTOM = 2;

    /** Same default LogoImage falls back to when the key is unset. */
    private static final int DEFAULT_COLOR = 0xff1a73e8;

    private MonetColorPickerPreference mCustomColorPref;
    private ListPreference mColorModePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_logo);
        getActivity().setTitle(R.string.yozakura_logo_title);

        mCustomColorPref = findPreference(KEY_CUSTOM_COLOR);
        mColorModePref = findPreference(KEY_COLOR_MODE);

        // Show the picked entry as the summary without ListPreference running
        // String.format() over it (same reasoning as the Monet page).
        setSimpleSummary(findPreference(KEY_STYLE));
        setSimpleSummary(findPreference(KEY_POSITION));
        setSimpleSummary(mColorModePref);

        if (mCustomColorPref != null) {
            mCustomColorPref.setOnPreferenceChangeListener(this);
        }
        if (mColorModePref != null) {
            mColorModePref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCustomColorFromSettings();
        updateCustomColorVisibility(getColorMode());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomColorPref) {
            // MonetColorPickerPreference hands back RRGGBB; LogoImage wants opaque ARGB.
            final int rgb = Color.parseColor("#" + (String) newValue) & 0x00FFFFFF;
            Settings.System.putIntForUser(getContext().getContentResolver(),
                    KEY_COLOR_PICKER, 0xFF000000 | rgb, UserHandle.USER_CURRENT);
            updateCustomColorSummary(rgb);
            return true;
        }
        if (preference == mColorModePref) {
            // The list widget persists the value itself; just follow it in the UI.
            updateCustomColorVisibility(parseInt((String) newValue, 0));
            return true;
        }
        return false;
    }

    private static void setSimpleSummary(Preference preference) {
        if (preference instanceof ListPreference) {
            preference.setSummaryProvider(
                    ListPreference.SimpleSummaryProvider.getInstance());
        }
    }

    private int getColorMode() {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                KEY_COLOR_MODE, 0, UserHandle.USER_CURRENT);
    }

    private void updateCustomColorVisibility(int colorMode) {
        if (mCustomColorPref != null) {
            mCustomColorPref.setVisible(colorMode == COLOR_MODE_CUSTOM);
        }
    }

    private void updateCustomColorFromSettings() {
        if (mCustomColorPref == null) {
            return;
        }
        final int argb = Settings.System.getIntForUser(getContext().getContentResolver(),
                KEY_COLOR_PICKER, DEFAULT_COLOR, UserHandle.USER_CURRENT);
        final int rgb = argb & 0x00FFFFFF;
        mCustomColorPref.setColorRgb(rgb);
        updateCustomColorSummary(rgb);
    }

    private void updateCustomColorSummary(int rgb) {
        mCustomColorPref.setSummary(String.format(Locale.US, "#%06X", rgb));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
