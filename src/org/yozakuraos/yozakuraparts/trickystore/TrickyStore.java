/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported from Infinity/InfinitySuite (Kotlin) to YozakuraParts (Java, classic
 * LineageParts preferences). The Compose per-app target picker
 * (TrickyStoreAppSettings) is intentionally NOT ported to avoid pulling the
 * settingslib.spa / Material3 Compose stack into YozakuraParts; target apps are
 * managed via file import plus a sensible default seeded on keybox import.
 */

package org.yozakuraos.yozakuraparts.trickystore;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class TrickyStore extends SettingsPreferenceFragment {

    private static final String KEYBOX_KEY = "spoof_trickystore_keybox";
    private static final String TARGET_KEY = "spoof_trickystore_target";
    private static final String PATCH_KEY = "spoof_trickystore_patch";

    private static final String VENDING_PACKAGE = "com.android.vending";
    private static final String DROIDGUARD_PACKAGE = "com.google.android.gms.unstable";
    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String RKPD_PACKAGE = "com.google.android.rkpdapp";

    private static final int KEYBOX_DOWNLOAD_TIMEOUT_MS = 10_000;

    // https://git.evolution-x.org/EvoX/keybox/raw/branch/main/keybox.xml
    private static final String KEYBOX_DOWNLOAD_URL = new String(Base64.decode(
            "aHR0cHM6Ly9naXQuZXZvbHV0aW9uLXgub3JnL0V2b1gva2V5Ym94L3Jhdy9icmFuY2gvbWFpbi9rZXlib3gueG1s",
            Base64.DEFAULT), StandardCharsets.UTF_8);

    // Seeded into the (empty) target list on first keybox import so attestation
    // spoofing takes effect for Play services without a separate target step.
    private static final String DEFAULT_TARGETS =
            GMS_PACKAGE + "\n"
            + VENDING_PACKAGE + "\n"
            + "com.google.android.gsf" + "\n"
            + "com.google.android.apps.walletnfcrel";

    private static final Pattern PATCH_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> mKeyboxPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                android.net.Uri uri = result.getData().getData();
                if (uri == null) return;
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    byte[] bytes = in != null ? in.readAllBytes() : new byte[0];
                    String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    Settings.Secure.putString(
                            requireContext().getContentResolver(), KEYBOX_KEY, encoded);
                    seedDefaultTargetsIfEmpty();
                    killGms();
                    toast(getString(R.string.ts_keybox_imported));
                    refreshStatus();
                } catch (Exception e) {
                    toast(getString(R.string.ts_failed, String.valueOf(e.getMessage())));
                }
            });

    private final ActivityResultLauncher<Intent> mTargetPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                android.net.Uri uri = result.getData().getData();
                if (uri == null) return;
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    byte[] bytes = in != null ? in.readAllBytes() : new byte[0];
                    String text = new String(bytes, StandardCharsets.UTF_8);
                    Settings.Secure.putString(
                            requireContext().getContentResolver(), TARGET_KEY, text);
                    toast(getString(R.string.ts_target_list_imported));
                    refreshStatus();
                } catch (Exception e) {
                    toast(getString(R.string.ts_failed, String.valueOf(e.getMessage())));
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tricky_store);

        Preference download = findPreference("ts_download_latest_keybox");
        if (download != null) {
            download.setOnPreferenceClickListener(p -> {
                downloadLatestKeybox(p);
                return true;
            });
        }

        Preference importKeybox = findPreference("ts_import_keybox");
        if (importKeybox != null) {
            importKeybox.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                mKeyboxPicker.launch(intent);
                return true;
            });
        }

        Preference deleteKeybox = findPreference("ts_delete_keybox");
        if (deleteKeybox != null) {
            deleteKeybox.setOnPreferenceClickListener(p -> {
                showDeleteKeyboxDialog();
                return true;
            });
        }

        Preference patch = findPreference("ts_security_patch");
        if (patch != null) {
            patch.setOnPreferenceClickListener(p -> {
                showPatchDateDialog();
                return true;
            });
        }

        Preference importTargets = findPreference("ts_import_targets");
        if (importTargets != null) {
            importTargets.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                mTargetPicker.launch(intent);
                return true;
            });
        }

        refreshStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    private String secure(String key) {
        return Settings.Secure.getString(requireContext().getContentResolver(), key);
    }

    private void refreshStatus() {
        boolean keyboxExists = secure(KEYBOX_KEY) != null && !secure(KEYBOX_KEY).isEmpty();

        String targetContent = secure(TARGET_KEY);
        int targetCount = 0;
        if (targetContent != null && !targetContent.isEmpty()) {
            for (String line : targetContent.split("\n")) {
                if (!line.trim().isEmpty()) targetCount++;
            }
        }

        Preference importKeybox = findPreference("ts_import_keybox");
        if (importKeybox != null) {
            importKeybox.setSummary(keyboxExists
                    ? getString(R.string.ts_keybox_installed)
                    : getString(R.string.ts_no_keybox));
        }

        Preference deleteKeybox = findPreference("ts_delete_keybox");
        if (deleteKeybox != null) {
            deleteKeybox.setEnabled(keyboxExists);
        }

        Preference importTargets = findPreference("ts_import_targets");
        if (importTargets != null) {
            importTargets.setSummary(targetCount > 0
                    ? getString(R.string.ts_target_apps_count, targetCount)
                    : getString(R.string.ts_no_targets));
        }

        String patchDate = secure(PATCH_KEY);
        Preference patch = findPreference("ts_security_patch");
        if (patch != null) {
            patch.setSummary(patchDate != null && !patchDate.isEmpty()
                    ? patchDate
                    : getString(R.string.ts_no_patch));
        }

        Preference verification = findPreference("ts_verification_mode");
        if (verification != null) {
            verification.setSummary(buildVerificationSummary());
        }
    }

    private String buildVerificationSummary() {
        String content = secure(TARGET_KEY);
        if (content == null || content.isEmpty()) {
            return getString(R.string.ts_verification_mode_auto);
        }

        int auto = 0, cert = 0, leaf = 0;
        for (String raw : content.split("\n")) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.endsWith("!")) cert++;
            else if (trimmed.endsWith("?")) leaf++;
            else auto++;
        }

        if (auto == 0 && cert == 0 && leaf == 0) {
            return getString(R.string.ts_verification_mode_auto);
        }

        StringBuilder sb = new StringBuilder();
        if (auto > 0) sb.append(getString(R.string.ts_verification_auto_count, auto));
        if (cert > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(getString(R.string.ts_verification_cert_count, cert));
        }
        if (leaf > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(getString(R.string.ts_verification_leaf_count, leaf));
        }
        return sb.toString();
    }

    private void seedDefaultTargetsIfEmpty() {
        String existing = secure(TARGET_KEY);
        if (existing == null || existing.trim().isEmpty()) {
            Settings.Secure.putString(
                    requireContext().getContentResolver(), TARGET_KEY, DEFAULT_TARGETS);
        }
    }

    private void showDeleteKeyboxDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ts_delete_keybox_title)
                .setMessage(R.string.ts_delete_keybox_message)
                .setPositiveButton(R.string.ts_delete, (d, w) -> {
                    try {
                        Settings.Secure.putString(
                                requireContext().getContentResolver(), KEYBOX_KEY, "");
                        toast(getString(R.string.ts_keybox_deleted));
                        refreshStatus();
                    } catch (Exception e) {
                        toast(getString(R.string.ts_failed, String.valueOf(e.getMessage())));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPatchDateDialog() {
        String current = secure(PATCH_KEY);
        if (current == null) current = "";
        final EditText input = new EditText(requireContext());
        input.setText(current);
        input.setHint(getString(R.string.ts_patch_date_hint));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(48, 24, 48, 24);

        final String currentFinal = current;
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ts_security_patch)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.ts_delete, (d, w) -> {
                    Settings.Secure.putString(
                            requireContext().getContentResolver(), PATCH_KEY, "");
                    refreshStatus();
                })
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty() && !PATCH_DATE.matcher(value).matches()) {
                    toast(getString(R.string.ts_invalid_patch_date));
                    return;
                }
                Settings.Secure.putString(
                        requireContext().getContentResolver(), PATCH_KEY, value);
                refreshStatus();
                dialog.dismiss();
            });
            if (currentFinal.isEmpty()) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
            }
        });

        dialog.show();
    }

    private void killGms() {
        try {
            ActivityManager am = requireContext().getSystemService(ActivityManager.class);
            am.forceStopPackage(VENDING_PACKAGE);
            am.forceStopPackage(DROIDGUARD_PACKAGE);
            am.forceStopPackage(GMS_PACKAGE);
            am.forceStopPackage(RKPD_PACKAGE);
            // Clear Play Store's cached attestation results so the new keybox
            // takes effect immediately (mirrors Specter's gms.sh).
            requireContext().getPackageManager()
                    .clearApplicationUserData(VENDING_PACKAGE, null);
        } catch (Exception ignored) {
        }
    }

    private void downloadLatestKeybox(final Preference pref) {
        pref.setEnabled(false);
        new Thread(() -> {
            final boolean success = downloadLatestKeyboxFile();
            mMainHandler.post(() -> {
                toast(getString(success
                        ? R.string.ts_download_keybox_success
                        : R.string.ts_download_keybox_error));
                pref.setEnabled(true);
            });
        }, "TrickyStore-KeyboxDownload").start();
    }

    private boolean downloadLatestKeyboxFile() {
        File outputDir = new File(Environment.getExternalStorageDirectory(), "InfinityResources");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return false;
        }
        File outputFile = new File(outputDir, "keybox.xml");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(KEYBOX_DOWNLOAD_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(KEYBOX_DOWNLOAD_TIMEOUT_MS);
            connection.setReadTimeout(KEYBOX_DOWNLOAD_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setDoInput(true);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            long startTime = SystemClock.elapsedRealtime();
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (SystemClock.elapsedRealtime() - startTime > KEYBOX_DOWNLOAD_TIMEOUT_MS) {
                        throw new IOException("Download timed out");
                    }
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return true;
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
