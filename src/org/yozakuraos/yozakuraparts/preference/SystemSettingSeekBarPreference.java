/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.yozakuraos.yozakuraparts.preference;

import android.content.Context;
import android.util.AttributeSet;

/** Settings.System counterpart of SecureSettingSeekBarPreference (cp121c). */
public class SystemSettingSeekBarPreference extends CustomSeekBarPreference {

    public SystemSettingSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }

    public SystemSettingSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }

    public SystemSettingSeekBarPreference(Context context) {
        super(context, null);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }
}
