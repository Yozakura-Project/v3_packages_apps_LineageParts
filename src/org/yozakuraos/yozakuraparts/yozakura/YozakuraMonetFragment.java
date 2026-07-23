/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * YozakuraOS cp78: expose the advanced Monet engine controls (theme style,
 * luminance / chroma factors, whole-palette tint, background tint) that the
 * cp77 framework changes in ThemeOverlayController already consume. Values are
 * written straight into THEME_CUSTOMIZATION_OVERLAY_PACKAGES, matching the JSON
 * keys the framework reads.
 *
 * cp85: add a free-form Monet seed color via MonetColorPickerPreference. Picking
 * a color writes the same accent_color / system_palette / color_source=preset
 * keys the cp79 accent presets use, so a custom color is just an arbitrary hex
 * instead of one of the twelve presets. No framework change.
 */
public class YozakuraMonetFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "YozakuraMonet";

    private static final String OVERLAY_THEME_STYLE =
            "android.theme.customization.theme_style";
    private static final String OVERLAY_ACCENT_COLOR =
            "android.theme.customization.accent_color";
    private static final String OVERLAY_SYSTEM_PALETTE =
            "android.theme.customization.system_palette";
    private static final String OVERLAY_COLOR_SOURCE =
            "android.theme.customization.color_source";
    private static final String COLOR_SOURCE_PRESET = "preset";
    private static final String COLOR_SOURCE_WALLPAPER = "home_wallpaper";
    private static final String VALUE_DYNAMIC = "dynamic";
    private static final String VALUE_CUSTOM = "custom";
    private static final String OVERLAY_LUMINANCE_FACTOR =
            "android.theme.customization.luminance_factor";
    private static final String OVERLAY_CHROMA_FACTOR =
            "android.theme.customization.chroma_factor";
    private static final String OVERLAY_WHOLE_PALETTE =
            "android.theme.customization.whole_palette";
    private static final String OVERLAY_TINT_BACKGROUND =
            "android.theme.customization.tint_background";
    private static final String TIMESTAMP_FIELD = "_applied_timestamp";

    private static final String PREF_ACCENT = "monet_accent_preset";
    private static final String PREF_CUSTOM_COLOR = "monet_custom_color";
    private static final String PREF_STYLE = "monet_theme_style";
    private static final String PREF_LUMINANCE = "monet_luminance_factor";
    private static final String PREF_CHROMA = "monet_chroma_factor";
    private static final String PREF_WHOLE_PALETTE = "monet_whole_palette";
    private static final String PREF_TINT_BACKGROUND = "monet_tint_background";

    private ListPreference mAccentPref;
    private MonetColorPickerPreference mCustomColorPref;
    private ListPreference mStylePref;
    private ListPreference mLuminancePref;
    private ListPreference mChromaPref;
    private SwitchPreferenceCompat mWholePalettePref;
    private SwitchPreferenceCompat mTintBackgroundPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_monet);
        getActivity().setTitle(R.string.yozakura_monet_title);

        mAccentPref = findPreference(PREF_ACCENT);
        mCustomColorPref = findPreference(PREF_CUSTOM_COLOR);
        mStylePref = findPreference(PREF_STYLE);
        mLuminancePref = findPreference(PREF_LUMINANCE);
        mChromaPref = findPreference(PREF_CHROMA);
        mWholePalettePref = findPreference(PREF_WHOLE_PALETTE);
        mTintBackgroundPref = findPreference(PREF_TINT_BACKGROUND);

        // Use the built-in summary provider so the selected entry is shown as the
        // summary without ListPreference running String.format() over it. Some
        // entries contain a literal '%' (e.g. "+15%"), which would otherwise crash
        // getSummary() with UnknownFormatConversionException.
        mAccentPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mStylePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mLuminancePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mChromaPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        mAccentPref.setOnPreferenceChangeListener(this);
        mCustomColorPref.setOnPreferenceChangeListener(this);
        mStylePref.setOnPreferenceChangeListener(this);
        mLuminancePref.setOnPreferenceChangeListener(this);
        mChromaPref.setOnPreferenceChangeListener(this);
        mWholePalettePref.setOnPreferenceChangeListener(this);
        mTintBackgroundPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private JSONObject getJson() {
        final String json = Settings.Secure.getStringForUser(
                getActivity().getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(json)) {
            return new JSONObject();
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e);
            return new JSONObject();
        }
    }

    private void putJson(JSONObject object) {
        try {
            object.putOpt(TIMESTAMP_FIELD, System.currentTimeMillis());
        } catch (JSONException ignored) {
        }
        Settings.Secure.putStringForUser(
                getActivity().getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                object.toString(), UserHandle.USER_CURRENT);
    }

    private void load() {
        final JSONObject object = getJson();

        final String source = object.optString(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_WALLPAPER);
        if (COLOR_SOURCE_PRESET.equals(source)) {
            final String hex = object.optString(OVERLAY_ACCENT_COLOR, "");
            setAccentValue(hex);
            updateCustomColorFromHex(hex);
        } else {
            setListValue(mAccentPref, VALUE_DYNAMIC);
            updateCustomColorFromHex("");
        }

        final String style = object.optString(OVERLAY_THEME_STYLE, "TONAL_SPOT");
        setListValue(mStylePref, style);

        setListByDouble(mLuminancePref, object.optDouble(OVERLAY_LUMINANCE_FACTOR, 1d));
        setListByDouble(mChromaPref, object.optDouble(OVERLAY_CHROMA_FACTOR, 1d));

        mWholePalettePref.setChecked(object.optInt(OVERLAY_WHOLE_PALETTE, 0) == 1);
        mTintBackgroundPref.setChecked(object.optInt(OVERLAY_TINT_BACKGROUND, 0) == 1);
    }

    /**
     * Select the preset whose hex matches the stored accent color. If the stored
     * color is a non-empty hex that is not one of the presets, it is a custom
     * color, so select the "custom" entry. Otherwise fall back to Dynamic.
     */
    private void setAccentValue(String hex) {
        if (!TextUtils.isEmpty(hex)) {
            for (CharSequence v : mAccentPref.getEntryValues()) {
                if (v.toString().equalsIgnoreCase(hex)) {
                    setListValue(mAccentPref, v.toString());
                    return;
                }
            }
            setListValue(mAccentPref, VALUE_CUSTOM);
            return;
        }
        setListValue(mAccentPref, VALUE_DYNAMIC);
    }

    /** Reflect the current seed hex in the custom-color row (picker start + summary). */
    private void updateCustomColorFromHex(String hex) {
        if (mCustomColorPref == null) {
            return;
        }
        if (TextUtils.isEmpty(hex)) {
            mCustomColorPref.setSummary(R.string.yozakura_monet_custom_color_summary);
            return;
        }
        try {
            mCustomColorPref.setColorRgb(Color.parseColor('#' + hex) & 0x00FFFFFF);
            mCustomColorPref.setSummary("#" + hex.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            mCustomColorPref.setSummary(R.string.yozakura_monet_custom_color_summary);
        }
    }

    private void setListValue(ListPreference pref, String value) {
        if (value == null) return;
        int index = pref.findIndexOfValue(value);
        if (index < 0) index = 0;
        pref.setValueIndex(index);
    }

    /** Pick the entry whose numeric value is closest to the stored factor. */
    private void setListByDouble(ListPreference pref, double value) {
        final CharSequence[] values = pref.getEntryValues();
        int best = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            double v;
            try {
                v = Double.parseDouble(values[i].toString());
            } catch (NumberFormatException e) {
                continue;
            }
            final double diff = Math.abs(v - value);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        pref.setValueIndex(best);
    }

    /** Apply a preset/custom seed hex to the three theme-customization keys. */
    private void applyAccentHex(String hex) {
        final JSONObject object = getJson();
        try {
            object.putOpt(OVERLAY_ACCENT_COLOR, hex);
            object.putOpt(OVERLAY_SYSTEM_PALETTE, hex);
            object.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_PRESET);
            putJson(object);
        } catch (JSONException e) {
            Log.i(TAG, "Failed to apply accent hex.", e);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final JSONObject object = getJson();
        try {
            if (preference == mAccentPref) {
                final String value = (String) newValue;
                if (VALUE_CUSTOM.equals(value)) {
                    // Selecting "custom" just opens the color picker; the actual
                    // write happens when the dialog is confirmed. Don't persist
                    // the list selection yet.
                    onDisplayPreferenceDialog(mCustomColorPref);
                    return false;
                }
                if (VALUE_DYNAMIC.equals(value)) {
                    object.remove(OVERLAY_ACCENT_COLOR);
                    object.remove(OVERLAY_SYSTEM_PALETTE);
                    object.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_WALLPAPER);
                    putJson(object);
                    setListValue(mAccentPref, value);
                    updateCustomColorFromHex("");
                } else {
                    object.putOpt(OVERLAY_ACCENT_COLOR, value);
                    object.putOpt(OVERLAY_SYSTEM_PALETTE, value);
                    object.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_PRESET);
                    putJson(object);
                    setListValue(mAccentPref, value);
                    updateCustomColorFromHex(value);
                }
                return true;
            } else if (preference == mCustomColorPref) {
                final String hex = (String) newValue; // RRGGBB
                applyAccentHex(hex);
                setAccentValue(hex);
                updateCustomColorFromHex(hex);
                return true;
            } else if (preference == mStylePref) {
                final String value = (String) newValue;
                object.putOpt(OVERLAY_THEME_STYLE, value);
                putJson(object);
                setListValue(mStylePref, value);
                return true;
            } else if (preference == mLuminancePref) {
                putFactor(object, OVERLAY_LUMINANCE_FACTOR, (String) newValue);
                putJson(object);
                setListValue(mLuminancePref, (String) newValue);
                return true;
            } else if (preference == mChromaPref) {
                putFactor(object, OVERLAY_CHROMA_FACTOR, (String) newValue);
                putJson(object);
                setListValue(mChromaPref, (String) newValue);
                return true;
            } else if (preference == mWholePalettePref) {
                putFlag(object, OVERLAY_WHOLE_PALETTE, (Boolean) newValue);
                putJson(object);
                return true;
            } else if (preference == mTintBackgroundPref) {
                putFlag(object, OVERLAY_TINT_BACKGROUND, (Boolean) newValue);
                putJson(object);
                return true;
            }
        } catch (JSONException | IllegalArgumentException e) {
            Log.i(TAG, "Failed to update Monet setting.", e);
        }
        return false;
    }

    private static void putFactor(JSONObject object, String key, String value)
            throws JSONException {
        final double d = Double.parseDouble(value);
        if (d == 1d) {
            object.remove(key);
        } else {
            object.putOpt(key, d);
        }
    }

    private static void putFlag(JSONObject object, String key, boolean enabled) {
        if (enabled) {
            try {
                object.putOpt(key, 1);
            } catch (JSONException ignored) {
            }
        } else {
            object.remove(key);
        }
    }
}
