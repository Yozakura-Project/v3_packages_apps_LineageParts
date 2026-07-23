/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.preference;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

import lineageos.preference.SelfRemovingListPreference;

/** ListPreference persisting to Settings.System by its key. */
public class SystemSettingListPreference extends SelfRemovingListPreference {

    public SystemSettingListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SystemSettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean isPersisted() {
        return Settings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    @Override
    protected void putString(String key, String value) {
        Settings.System.putString(getContext().getContentResolver(), key, value);
    }

    @Override
    protected String getString(String key, String defaultValue) {
        final String v = Settings.System.getString(getContext().getContentResolver(), key);
        return v != null ? v : defaultValue;
    }
}
