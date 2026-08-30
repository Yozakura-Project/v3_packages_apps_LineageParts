/*
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Per-app picker for the AxSandbox lock/hide lists. Talks to the framework
 * service through AxSandboxManager; the AIDL side is complete, so the only work
 * here is presenting getLockablePackages() and calling the add/remove methods.
 */

package org.yozakuraos.yozakuraparts.applocker;

import android.app.AxSandboxManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AppLockerPackageList extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private AxSandboxManager mManager;

    /** Title shown for this list. */
    protected abstract int getTitleResId();

    /** Packages that should start out checked. */
    protected abstract List<String> getSelectedPackages(AxSandboxManager manager);

    protected abstract void onSelected(AxSandboxManager manager, String packageName);

    protected abstract void onDeselected(AxSandboxManager manager, String packageName);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
        getActivity().setTitle(getTitleResId());

        mManager = (AxSandboxManager)
                getActivity().getSystemService(Context.AX_SANDBOX_SERVICE);
        if (mManager == null) {
            // Registered in SystemServiceRegistry; a null here means the framework
            // side is missing, not that the user has nothing to configure.
            Toast.makeText(getActivity(), R.string.applock_service_unavailable,
                    Toast.LENGTH_LONG).show();
            return;
        }
        populate();
    }

    private void populate() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final PackageManager pm = getActivity().getPackageManager();
        final List<String> lockable = mManager.getLockablePackages();
        final Set<String> selected = new HashSet<>(getSelectedPackages(mManager));

        final List<Entry> entries = new ArrayList<>(lockable.size());
        for (String pkg : lockable) {
            entries.add(new Entry(pkg, labelOf(pm, pkg)));
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return collator.compare(a.label, b.label);
            }
        });

        for (Entry entry : entries) {
            final CheckBoxPreference pref = new CheckBoxPreference(getActivity());
            pref.setKey(entry.packageName);
            pref.setTitle(entry.label);
            pref.setSummary(entry.packageName);
            pref.setPersistent(false);
            pref.setChecked(selected.contains(entry.packageName));
            try {
                pref.setIcon(pm.getApplicationIcon(entry.packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
                // Uninstalled between getLockablePackages() and here; leave the
                // default icon rather than dropping the row.
            }
            pref.setOnPreferenceChangeListener(this);
            screen.addPreference(pref);
        }
    }

    private static String labelOf(PackageManager pm, String packageName) {
        try {
            final ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mManager == null) return false;
        final String pkg = preference.getKey();
        if ((Boolean) newValue) {
            onSelected(mManager, pkg);
        } else {
            onDeselected(mManager, pkg);
        }
        return true;
    }

    private static final class Entry {
        final String packageName;
        final String label;

        Entry(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
