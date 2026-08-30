/*
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.yozakuraos.yozakuraparts.applocker;

import android.app.AxSandboxManager;

import org.yozakuraos.yozakuraparts.R;

import java.util.List;

/**
 * Apps hidden outright: ActivityStarter rejects the launch with
 * START_CLASS_NOT_FOUND, so they behave as if they were not installed.
 */
public class AppLockerHiddenList extends AppLockerPackageList {

    @Override
    protected int getTitleResId() {
        return R.string.applock_hidden_list;
    }

    @Override
    protected List<String> getSelectedPackages(AxSandboxManager manager) {
        return manager.getHiddenPackages();
    }

    @Override
    protected void onSelected(AxSandboxManager manager, String packageName) {
        manager.setPackageHidden(packageName, true);
    }

    @Override
    protected void onDeselected(AxSandboxManager manager, String packageName) {
        manager.setPackageHidden(packageName, false);
    }
}
