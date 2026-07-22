/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.statusbar.IStatusBarService;

/** Shared helpers for YozakuraParts. */
public final class YozakuraUtils {

    private YozakuraUtils() {}

    /** Restart SystemUI so a changed setting applies immediately. */
    public static void restartSystemUI() {
        try {
            final IStatusBarService bar = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            if (bar != null) {
                bar.restartSystemUI();
            }
        } catch (RemoteException e) {
        }
    }

    /**
     * Restart SystemUI after a short delay, giving a SecureSettingListPreference
     * time to persist its new value to Settings.Secure first.
     */
    public static void restartSystemUIDelayed() {
        new Handler(Looper.getMainLooper()).postDelayed(
                YozakuraUtils::restartSystemUI, 300);
    }
}
