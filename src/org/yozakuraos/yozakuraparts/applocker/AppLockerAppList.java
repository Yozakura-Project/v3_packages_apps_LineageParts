/*
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.yozakuraos.yozakuraparts.applocker;

import android.app.AxSandboxManager;

import org.yozakuraos.yozakuraparts.R;

import java.util.List;

/** Apps that require the credential before they will launch. */
public class AppLockerAppList extends AppLockerPackageList {

    @Override
    protected int getTitleResId() {
        return R.string.applock_app_list;
    }

    @Override
    protected List<String> getSelectedPackages(AxSandboxManager manager) {
        return manager.getLockedPackages();
    }

    @Override
    protected void onSelected(AxSandboxManager manager, String packageName) {
        manager.addLockedApp(packageName);
    }

    @Override
    protected void onDeselected(AxSandboxManager manager, String packageName) {
        manager.removeLockedApp(packageName);
    }
}
