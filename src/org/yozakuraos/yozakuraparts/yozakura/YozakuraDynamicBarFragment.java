/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONArray;
import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * YozakuraOS cp109/cp110: DynamicBar (Dynamic Island) settings page under the Yozakura
 * status bar menu. The bar itself was ported in cp103-cp108 (SystemUI core, keyguard chip,
 * notification routing), all gated by Settings.Secure ax_dynamic_bar_enabled (default off).
 *
 * The master, keyguard and compact-notification switches plus the battery-chip list are
 * self-binding lineageos.preference / in-tree widgets that write straight into android
 * Settings.Secure using the exact keys AxDynamicBarSettings observes. The per-event
 * SwitchPreferenceCompat controls are driven here: their state mirrors the
 * ax_dynamic_bar_events JSON array of disabled event ids (empty or absent = all enabled),
 * so toggling one rewrites that array. Mirrors the InfinitySuite DynamicBar fragment.
 */
public class YozakuraDynamicBarFragment extends SettingsPreferenceFragment {

    private static final String KEY_EVENTS = "ax_dynamic_bar_events";
    private static final String KEY_COMPACT = "ax_dynamic_bar_compact_notifications";

    // Event ids matching SystemUI axdynamicbar EVENT_TYPE_IDS and the event_* preference keys.
    private static final String[] EVENT_TYPE_IDS = {
        "media", "call", "charging", "notification", "timer", "alarm", "stopwatch",
        "bluetooth", "hotspot", "ringer", "vpn", "clipboard", "torch",
        "audio_recording", "biometric_unlock",
    };

    private ContentResolver resolver() {
        return getContext().getContentResolver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_dynamic_bar);
        getActivity().setTitle(R.string.yozakura_dynamic_bar_title);

        setupEventToggles();
        updateCompactVisibility();
    }

    private void setupEventToggles() {
        final Set<String> disabled = getDisabledEvents();
        for (final String typeId : EVENT_TYPE_IDS) {
            final SwitchPreferenceCompat pref = findPreference("event_" + typeId);
            if (pref == null) continue;
            pref.setChecked(!disabled.contains(typeId));
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                toggleEvent(typeId, (Boolean) newValue);
                if ("notification".equals(typeId)) {
                    updateCompactVisibility();
                }
                return true;
            });
        }
    }

    private Set<String> getDisabledEvents() {
        final Set<String> out = new LinkedHashSet<>();
        final String json = Settings.Secure.getStringForUser(
                resolver(), KEY_EVENTS, UserHandle.USER_CURRENT);
        if (json == null || json.trim().isEmpty()) return out;
        try {
            final JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                final String v = arr.optString(i, null);
                if (v != null && !v.isEmpty()) out.add(v);
            }
        } catch (Exception ignored) {
            // Malformed value -> treat as no events disabled.
        }
        return out;
    }

    private void toggleEvent(String typeId, boolean enabled) {
        final Set<String> updated = getDisabledEvents();
        if (enabled) {
            updated.remove(typeId);
        } else {
            updated.add(typeId);
        }
        final String json = updated.isEmpty() ? "" : new JSONArray(updated).toString();
        Settings.Secure.putStringForUser(
                resolver(), KEY_EVENTS, json, UserHandle.USER_CURRENT);
    }

    private void updateCompactVisibility() {
        final Preference compact = findPreference(KEY_COMPACT);
        final SwitchPreferenceCompat notif = findPreference("event_notification");
        if (compact != null && notif != null) {
            compact.setVisible(notif.isChecked());
        }
    }
}
