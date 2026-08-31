/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.settingslib.widget.LayoutPreference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

public class YozakuraSettings extends SettingsPreferenceFragment {

    private static final String TAG = "YozakuraSettings";

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

        // Category tiles.
        setFragmentTile(bento, R.id.yozakura_tile_statusbar,
                PKG + ".yozakura.YozakuraStatusBarFragment");
        setFragmentTile(bento, R.id.yozakura_tile_clock,
                PKG + ".yozakura.YozakuraClockFragment");
        setFragmentTile(bento, R.id.yozakura_tile_lockscreen,
                PKG + ".yozakura.YozakuraLockScreenFragment");
        setFragmentTile(bento, R.id.yozakura_tile_power,
                PKG + ".yozakura.YozakuraPowerFragment");
        setFragmentTile(bento, R.id.yozakura_tile_misc,
                PKG + ".yozakura.YozakuraMiscFragment");
        setFragmentTile(bento, R.id.yozakura_tile_display,
                PKG + ".yozakura.YozakuraDisplayFragment");
        setFragmentTile(bento, R.id.yozakura_tile_input,
                PKG + ".yozakura.YozakuraInputFragment");

        // Monet lives under Display, but it is the tile people come here for, so
        // it gets promoted onto the sheet with the live accent ramp on it.
        setFragmentTile(bento, R.id.yozakura_tile_monet,
                PKG + ".yozakura.YozakuraMonetFragment");

        // Wallpaper is the system picker, not one of our screens.
        setTileClick(bento, R.id.yozakura_tile_wallpaper, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWallpaperPicker();
            }
        });

        // The lock tile previews the lock wallpaper, the wallpaper tile the home one.
        final View lockPreview = bento.findViewById(R.id.yozakura_lock_preview);
        if (lockPreview instanceof WallpaperPreviewView) {
            ((WallpaperPreviewView) lockPreview).setUseLockWallpaper(true);
        }
        final View homePreview = bento.findViewById(R.id.yozakura_wallpaper_preview);
        if (homePreview instanceof WallpaperPreviewView) {
            ((WallpaperPreviewView) homePreview).setUseLockWallpaper(false);
        }
    }

    private void openWallpaperPicker() {
        final Intent i = new Intent(Intent.ACTION_SET_WALLPAPER);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(i, getString(R.string.yozakura_wallpaper_title)));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No wallpaper picker on this build", e);
        }
    }

    private void setFragmentTile(LayoutPreference bento, int id, final String fragment) {
        setTileClick(bento, id, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent i = new Intent();
                i.setClassName(PKG, PKG + ".PartsActivity");
                i.putExtra(EXTRA_SHOW_FRAGMENT, fragment);
                startActivity(i);
            }
        });
    }

    private void setTileClick(LayoutPreference bento, int id, View.OnClickListener l) {
        final View v = bento.findViewById(id);
        if (v == null) {
            return;
        }
        v.setOnClickListener(l);
    }
}
