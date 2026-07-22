/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.android.settingslib.widget.LayoutPreference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

public class YozakuraSettings extends SettingsPreferenceFragment {

    private static final String PKG = "org.yozakuraos.yozakuraparts";
    private static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_settings);
        getActivity().setTitle(R.string.yozakura_settings_title);
        wireBento();
    }

    private void wireBento() {
        final LayoutPreference bento = (LayoutPreference) findPreference("yozakura_bento");
        if (bento == null) {
            return;
        }
        setTileClick(bento, R.id.yozakura_tile_statusbar,
                PKG + ".yozakura.YozakuraStatusBarFragment");
        setTileClick(bento, R.id.yozakura_tile_clock,
                PKG + ".yozakura.YozakuraClockFragment");
        setTileClick(bento, R.id.yozakura_tile_lockscreen,
                PKG + ".yozakura.YozakuraLockScreenFragment");
        setTileClick(bento, R.id.yozakura_tile_power,
                PKG + ".yozakura.YozakuraPowerFragment");
        setTileClick(bento, R.id.yozakura_tile_misc,
                PKG + ".yozakura.YozakuraMiscFragment");
        setTileClick(bento, R.id.yozakura_tile_display,
                PKG + ".yozakura.YozakuraDisplayFragment");
    }

    private void setTileClick(LayoutPreference bento, int id, final String fragment) {
        final View v = bento.findViewById(id);
        if (v == null) {
            return;
        }
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent i = new Intent();
                i.setClassName(PKG, PKG + ".PartsActivity");
                i.putExtra(EXTRA_SHOW_FRAGMENT, fragment);
                startActivity(i);
            }
        });
    }
}
