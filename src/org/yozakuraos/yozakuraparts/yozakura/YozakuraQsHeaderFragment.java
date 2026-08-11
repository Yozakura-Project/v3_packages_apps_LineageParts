/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YozakuraOS cp121c: QS header image settings page. The image was ported in cp121a and
 * wired into the shade in cp121b (SystemUI com.android.systemui.infinity.header).
 *
 * The switch, height and shadow are self-binding widgets writing into Settings.System, so
 * this fragment only drives what they cannot: the source and pack lists, whose entries
 * depend on what is installed, and copying a picked image somewhere StatusBarHeaderMachine
 * can read it.
 *
 * Adapted from Infinity's QsHeaderImageSettings. Two of its rows are gone: the "static"
 * source, which names a drawable as "package/name", and the Browse row, which opens the
 * org.omnirom.omnistyle header pack app. Neither is settable from here without that app,
 * so both would be dead controls. Header packs are still supported - anything answering
 * org.omnirom.DaylightHeaderPack shows up in the pack list - but until one is installed
 * the daylight source has no images and is left out of the source list. That is also why
 * the shade draws nothing on a fresh install with the header simply switched on.
 */
public class YozakuraQsHeaderFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_SOURCE = "qs_header_source";
    private static final String KEY_PACK = "daylight_header_pack";
    private static final String KEY_FILE_SELECT = "file_header_select";

    private static final String PROVIDER_DAYLIGHT = "daylight";
    private static final String PROVIDER_FILE = "file";

    private static final int REQUEST_PICK_IMAGE = 10001;

    /** Where the picked image is kept, so SystemUI can read it back by URI. */
    private static final String QSHEADER_RELATIVE_PATH = "Pictures/QSHeader";
    private static final String QSHEADER_DISPLAY_NAME = "qs_header_image";

    private ListPreference mSource;
    private ListPreference mPack;
    private Preference mFileSelect;

    private boolean mHasPacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.yozakura_qs_header);
        getActivity().setTitle(R.string.yozakura_qs_header_title);

        mSource = findPreference(KEY_SOURCE);
        mPack = findPreference(KEY_PACK);
        mFileSelect = findPreference(KEY_FILE_SELECT);

        final List<String> packEntries = new ArrayList<>();
        final List<String> packValues = new ArrayList<>();
        getAvailableHeaderPacks(packEntries, packValues);
        mHasPacks = !packEntries.isEmpty();

        mPack.setEntries(packEntries.toArray(new String[0]));
        mPack.setEntryValues(packValues.toArray(new String[0]));
        mPack.setVisible(mHasPacks);
        mPack.setOnPreferenceChangeListener(this);

        buildSourceList();
        mSource.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        final String provider = getProvider();
        setListValue(mSource, provider);
        setListValue(mPack, Settings.System.getString(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK));
        updateRows(provider);
        updatePickSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getContext().getContentResolver();
        if (preference == mSource) {
            final String provider = (String) newValue;
            Settings.System.putString(resolver,
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, provider);
            setListValue(mSource, provider);
            updateRows(provider);
            return true;
        }
        if (preference == mPack) {
            Settings.System.putString(resolver,
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, (String) newValue);
            setListValue(mPack, (String) newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mFileSelect) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.yozakura_qs_header_pick_no_gallery,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PICK_IMAGE || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        final ContentResolver resolver = getContext().getContentResolver();
        deleteExistingHeaderImages(resolver);

        final Uri outUri = createHeaderImage(resolver, data.getData());
        if (outUri == null) {
            Toast.makeText(getContext(), R.string.yozakura_qs_header_pick_failed,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Picking a picture is also how the file source gets chosen; asking the user to
        // then find the source list would be busy work.
        //
        // Order matters: StatusBarHeaderMachine hands a settings change to whichever
        // provider is current, so the provider has to be file before the URI lands. The
        // other way round the picture is offered to the outgoing provider, FileHeaderProvider
        // never copies it, and enableProvider() finds nothing to load - every setting reads
        // back correctly and the shade stays empty.
        Settings.System.putString(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, PROVIDER_FILE);
        Settings.System.putString(resolver,
                Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, outUri.toString());
        setListValue(mSource, PROVIDER_FILE);
        updateRows(PROVIDER_FILE);
        updatePickSummary();
    }

    /**
     * The source list only offers what can actually produce an image: a picture from the
     * gallery always, header packs when one is installed.
     */
    private void buildSourceList() {
        final List<String> entries = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        if (mHasPacks) {
            entries.add(getString(R.string.yozakura_qs_header_source_daylight));
            values.add(PROVIDER_DAYLIGHT);
        }
        entries.add(getString(R.string.yozakura_qs_header_source_file));
        values.add(PROVIDER_FILE);

        mSource.setEntries(entries.toArray(new String[0]));
        mSource.setEntryValues(values.toArray(new String[0]));
        // Nothing to choose between when packs are absent.
        mSource.setVisible(mHasPacks);
    }

    private String getProvider() {
        final String provider = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER);
        if (provider != null) {
            return provider;
        }
        // StatusBarHeaderMachine falls back to daylight, which is useless without a pack.
        return mHasPacks ? PROVIDER_DAYLIGHT : PROVIDER_FILE;
    }

    private void updateRows(String provider) {
        mPack.setVisible(mHasPacks && PROVIDER_DAYLIGHT.equals(provider));
        // Without packs the source list is hidden, so the picker has to stay reachable
        // whatever the stored provider says - otherwise a stale "daylight" would leave the
        // page with nothing on it. Picking a picture writes the provider back to file.
        mFileSelect.setVisible(!mHasPacks || PROVIDER_FILE.equals(provider));
    }

    private void updatePickSummary() {
        final String uri = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_FILE_HEADER_IMAGE);
        mFileSelect.setSummary(uri == null
                ? getString(R.string.yozakura_qs_header_pick_none)
                : getString(R.string.yozakura_qs_header_pick_summary));
    }

    private static void setListValue(ListPreference preference, String value) {
        if (value == null) {
            return;
        }
        final int index = preference.findIndexOfValue(value);
        if (index < 0) {
            return;
        }
        preference.setValueIndex(index);
        preference.setSummary(preference.getEntry());
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        final Map<String, String> headerMap = new HashMap<>();
        final PackageManager packageManager = getContext().getPackageManager();
        final Intent intent = new Intent();

        intent.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(intent, 0)) {
            final String packageName = r.activityInfo.packageName;
            final CharSequence label = r.activityInfo.loadLabel(packageManager);
            headerMap.put(label == null ? packageName : label.toString(), packageName);
        }

        intent.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(intent, 0)) {
            if (r.activityInfo.name.endsWith(".theme")) {
                continue;
            }
            final String packageName = r.activityInfo.packageName;
            final CharSequence label = r.activityInfo.loadLabel(packageManager);
            headerMap.put(label == null ? packageName : label.toString(),
                    packageName + "/" + r.activityInfo.name);
        }

        final List<String> labels = new ArrayList<>(headerMap.keySet());
        Collections.sort(labels);
        for (String label : labels) {
            entries.add(label);
            values.add(headerMap.get(label));
        }
    }

    private void deleteExistingHeaderImages(ContentResolver resolver) {
        final String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?";
        final String[] args = new String[]{
                QSHEADER_RELATIVE_PATH + "/",
                QSHEADER_DISPLAY_NAME + "%"
        };
        try {
            resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, args);
        } catch (Exception ignored) {
        }
    }

    private Uri createHeaderImage(ContentResolver resolver, Uri inUri) {
        String mime = resolver.getType(inUri);
        if (mime == null) {
            mime = "image/*";
        }

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, QSHEADER_DISPLAY_NAME);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, QSHEADER_RELATIVE_PATH);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri outUri = null;
        try {
            outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (outUri == null) {
                return null;
            }
            try (InputStream in = resolver.openInputStream(inUri);
                 OutputStream out = resolver.openOutputStream(outUri, "wt")) {
                if (in == null || out == null) {
                    return null;
                }
                final byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
            final ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(outUri, done, null, null);
            return outUri;
        } catch (Exception e) {
            if (outUri != null) {
                try {
                    resolver.delete(outUri, null, null);
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }
}
