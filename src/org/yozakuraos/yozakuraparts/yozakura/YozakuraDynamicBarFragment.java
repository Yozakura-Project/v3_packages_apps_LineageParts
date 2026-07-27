/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.os.Bundle;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

/**
 * YozakuraOS cp109: expose the DynamicBar (Dynamic Island) settings page under the
 * Yozakura status bar menu. The bar itself was ported in cp103-cp108 (SystemUI
 * core, keyguard chip, notification routing), all gated by Settings.Secure
 * ax_dynamic_bar_enabled (default off).
 *
 * All controls are self-binding lineageos.preference widgets that write straight
 * into android Settings.Secure using the exact keys AxDynamicBarSettings observes
 * (ax_dynamic_bar_enabled / _keyguard_enabled / _compact_notifications), so no glue
 * code is needed. The bare (non-"Lineage") widgets target android Settings.Secure;
 * a "Lineage" list/switch would persist to the lineagesettings provider and the
 * consumer would never see the value.
 */
public class YozakuraDynamicBarFragment extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_dynamic_bar);
        getActivity().setTitle(R.string.yozakura_dynamic_bar_title);
    }
}
