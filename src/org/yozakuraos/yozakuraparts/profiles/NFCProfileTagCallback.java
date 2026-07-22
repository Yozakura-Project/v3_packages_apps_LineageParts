/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.profiles;

import android.nfc.Tag;

public interface NFCProfileTagCallback {
    void onTagRead(Tag tag);
}
