/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.preference;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

/** EditTextPreference that persists to Settings.Secure by its key. */
public class SecureSettingEditTextPreference extends EditTextPreference {

    public SecureSettingEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SecureSettingEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SecureSettingEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        Settings.Secure.putString(getContext().getContentResolver(), getKey(), value);
        return true;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        final String v = Settings.Secure.getString(
                getContext().getContentResolver(), getKey());
        return v != null ? v : defaultReturnValue;
    }
}
