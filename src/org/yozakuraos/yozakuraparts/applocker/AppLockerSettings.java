/*
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Settings front-end for the AxSandbox per-app lock.
 *
 * The framework service (AxSandboxService) blocks the launch of a locked app and
 * starts com.android.applocker/.AuthenticateActivity to ask for a credential.
 * AppLocker itself has no way to *create* that credential - AuthenticateActivity
 * calls unlockAndFinish() straight away when SandboxSecurityManager.isSetup() is
 * false, so without this screen a locked app opens for anyone. The setup UI lives
 * in Infinity's Sandbox app, which we did not port.
 *
 * Credential storage has to match SandboxSecurityManager exactly:
 *   sandbox_security_type   Secure  "NONE" | "PIN" | "PASSWORD" | "PATTERN"
 *   sandbox_credential_hash Secure  lower-case hex SHA-256 of the credential
 * Pattern is not offered here: it needs a 3x3 grid input and the hashed string is
 * the selected dot indices joined by ",". AppLocker can still verify one if some
 * other component ever sets it.
 */

package org.yozakuraos.yozakuraparts.applocker;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppLockerSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // Keys read by SandboxSecurityManager (AppLocker) and AxSandboxService.
    private static final String KEY_SECURITY_TYPE = "sandbox_security_type";
    private static final String KEY_CREDENTIAL_HASH = "sandbox_credential_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "sandbox_biometric_enabled";
    private static final String KEY_PREFER_BIOMETRIC = "sandbox_prefer_biometric";
    private static final String KEY_LOCK_BEHAVIOR = "sandbox_locked_app_behavior";
    private static final String KEY_LOCK_TIMEOUT = "sandbox_locked_app_timeout";

    private static final String TYPE_NONE = "NONE";
    private static final String TYPE_PIN = "PIN";
    private static final String TYPE_PASSWORD = "PASSWORD";
    private static final String TYPE_PATTERN = "PATTERN";

    // SandboxSecurityManager.MIN_PIN_LENGTH / MAX_PIN_LENGTH / MIN_PASSWORD_LENGTH
    private static final int PIN_LENGTH = 4;
    private static final int MIN_PASSWORD_LENGTH = 4;
    // AxSandboxManager.DEFAULT_LOCK_TIMEOUT
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    // AxSandboxManager.LOCK_BEHAVIOR_TIMEOUT
    private static final int BEHAVIOR_TIMEOUT = 1;

    private static final String PREF_SET = "applock_set_credential";
    private static final String PREF_CLEAR = "applock_clear_credential";
    private static final String PREF_BIOMETRIC = "applock_biometric";
    private static final String PREF_PREFER_BIOMETRIC = "applock_prefer_biometric";
    private static final String PREF_BEHAVIOR = "applock_behavior";
    private static final String PREF_TIMEOUT = "applock_timeout";

    private Preference mSetCredential;
    private Preference mClearCredential;
    private SwitchPreference mBiometric;
    private SwitchPreference mPreferBiometric;
    private ListPreference mBehavior;
    private Preference mTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.applocker_settings);

        mSetCredential = findPreference(PREF_SET);
        mClearCredential = findPreference(PREF_CLEAR);
        mBiometric = findPreference(PREF_BIOMETRIC);
        mPreferBiometric = findPreference(PREF_PREFER_BIOMETRIC);
        mBehavior = findPreference(PREF_BEHAVIOR);
        mTimeout = findPreference(PREF_TIMEOUT);

        mBiometric.setOnPreferenceChangeListener(this);
        mPreferBiometric.setOnPreferenceChangeListener(this);
        mBehavior.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private ContentResolver resolver() {
        return getActivity().getContentResolver();
    }

    private String securityType() {
        String type = Settings.Secure.getString(resolver(), KEY_SECURITY_TYPE);
        return TextUtils.isEmpty(type) ? TYPE_NONE : type;
    }

    private boolean isSetup() {
        return !TYPE_NONE.equals(securityType())
                && !TextUtils.isEmpty(Settings.Secure.getString(resolver(), KEY_CREDENTIAL_HASH));
    }

    private void refresh() {
        final boolean setup = isSetup();
        final String type = securityType();

        int summary;
        if (!setup) {
            summary = R.string.applock_credential_none;
        } else if (TYPE_PIN.equals(type)) {
            summary = R.string.applock_credential_pin;
        } else if (TYPE_PASSWORD.equals(type)) {
            summary = R.string.applock_credential_password;
        } else if (TYPE_PATTERN.equals(type)) {
            summary = R.string.applock_credential_pattern;
        } else {
            summary = R.string.applock_credential_none;
        }
        mSetCredential.setSummary(summary);
        mClearCredential.setEnabled(setup);

        final boolean hasBiometricHw = hasBiometricHardware();
        mBiometric.setEnabled(setup && hasBiometricHw);
        mBiometric.setChecked(setup && hasBiometricHw
                && Settings.Secure.getInt(resolver(), KEY_BIOMETRIC_ENABLED, 0) == 1);
        mPreferBiometric.setChecked(
                Settings.Secure.getInt(resolver(), KEY_PREFER_BIOMETRIC, 0) == 1);
        if (!hasBiometricHw) {
            mBiometric.setSummary(R.string.applock_biometric_unavailable);
        } else {
            mBiometric.setSummary(R.string.applock_biometric_summary);
        }

        final int behavior = Settings.Secure.getInt(resolver(), KEY_LOCK_BEHAVIOR, 0);
        mBehavior.setValue(String.valueOf(behavior));
        mBehavior.setSummary(mBehavior.getEntry());

        final int timeout = Settings.Secure.getInt(
                resolver(), KEY_LOCK_TIMEOUT, DEFAULT_TIMEOUT_SECONDS);
        mTimeout.setSummary(getString(R.string.applock_timeout_summary, timeout));
        // The timeout only means anything for LOCK_BEHAVIOR_TIMEOUT.
        mTimeout.setEnabled(behavior == BEHAVIOR_TIMEOUT);
    }

    private boolean hasBiometricHardware() {
        final PackageManager pm = getActivity().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                || pm.hasSystemFeature(PackageManager.FEATURE_FACE);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (PREF_SET.equals(key)) {
            showTypeChooser();
            return true;
        } else if (PREF_CLEAR.equals(key)) {
            confirmClear();
            return true;
        } else if (PREF_TIMEOUT.equals(key)) {
            showTimeoutDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (PREF_BIOMETRIC.equals(key)) {
            final boolean enabled = (Boolean) newValue;
            Settings.Secure.putInt(resolver(), KEY_BIOMETRIC_ENABLED, enabled ? 1 : 0);
            if (!enabled) {
                // Mirrors SandboxSecurityManager.setBiometricEnabled(false).
                Settings.Secure.putInt(resolver(), KEY_PREFER_BIOMETRIC, 0);
                mPreferBiometric.setChecked(false);
            }
            return true;
        } else if (PREF_PREFER_BIOMETRIC.equals(key)) {
            Settings.Secure.putInt(
                    resolver(), KEY_PREFER_BIOMETRIC, ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (PREF_BEHAVIOR.equals(key)) {
            int behavior;
            try {
                behavior = Integer.parseInt((String) newValue);
            } catch (NumberFormatException e) {
                return false;
            }
            Settings.Secure.putInt(resolver(), KEY_LOCK_BEHAVIOR, behavior);
            mBehavior.setValue((String) newValue);
            mBehavior.setSummary(mBehavior.getEntry());
            mTimeout.setEnabled(behavior == BEHAVIOR_TIMEOUT);
            return true;
        }
        return false;
    }

    private void showTypeChooser() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.applock_set_credential)
                .setItems(R.array.applock_credential_type_entries, (dialog, which) -> {
                    if (which == 0) {
                        showCredentialDialog(TYPE_PIN);
                    } else {
                        showCredentialDialog(TYPE_PASSWORD);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCredentialDialog(String type) {
        final boolean pin = TYPE_PIN.equals(type);
        final EditText input = new EditText(getActivity());
        input.setInputType(pin
                ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        input.setHint(pin ? R.string.applock_hint_pin : R.string.applock_hint_password);

        new AlertDialog.Builder(getActivity())
                .setTitle(pin ? R.string.applock_credential_pin : R.string.applock_credential_password)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final String first = input.getText().toString();
                    if (!validate(type, first)) return;
                    confirmCredential(type, first);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmCredential(String type, String first) {
        final boolean pin = TYPE_PIN.equals(type);
        final EditText input = new EditText(getActivity());
        input.setInputType(pin
                ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        input.setHint(R.string.applock_hint_confirm);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.applock_confirm_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (!first.equals(input.getText().toString())) {
                        toast(R.string.applock_error_mismatch);
                        return;
                    }
                    save(type, first);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean validate(String type, String credential) {
        if (TYPE_PIN.equals(type)) {
            if (credential.length() != PIN_LENGTH) {
                toast(R.string.applock_error_pin_length);
                return false;
            }
            for (int i = 0; i < credential.length(); i++) {
                if (!Character.isDigit(credential.charAt(i))) {
                    toast(R.string.applock_error_pin_digits);
                    return false;
                }
            }
            return true;
        }
        if (credential.length() < MIN_PASSWORD_LENGTH) {
            toast(R.string.applock_error_password_length);
            return false;
        }
        return true;
    }

    private void save(String type, String credential) {
        final String hash = hash(credential);
        if (hash == null) {
            toast(R.string.applock_error_generic);
            return;
        }
        Settings.Secure.putString(resolver(), KEY_SECURITY_TYPE, type);
        Settings.Secure.putString(resolver(), KEY_CREDENTIAL_HASH, hash);
        toast(R.string.applock_credential_saved);
        refresh();
    }

    private void confirmClear() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.applock_clear_credential)
                .setMessage(R.string.applock_clear_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Settings.Secure.putString(resolver(), KEY_SECURITY_TYPE, TYPE_NONE);
                    Settings.Secure.putString(resolver(), KEY_CREDENTIAL_HASH, null);
                    Settings.Secure.putInt(resolver(), KEY_BIOMETRIC_ENABLED, 0);
                    Settings.Secure.putInt(resolver(), KEY_PREFER_BIOMETRIC, 0);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showTimeoutDialog() {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(Settings.Secure.getInt(
                resolver(), KEY_LOCK_TIMEOUT, DEFAULT_TIMEOUT_SECONDS)));

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.applock_timeout)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int seconds;
                    try {
                        seconds = Integer.parseInt(input.getText().toString());
                    } catch (NumberFormatException e) {
                        toast(R.string.applock_error_generic);
                        return;
                    }
                    if (seconds < 0) {
                        toast(R.string.applock_error_generic);
                        return;
                    }
                    Settings.Secure.putInt(resolver(), KEY_LOCK_TIMEOUT, seconds);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toast(int resId) {
        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Must stay byte-for-byte compatible with SandboxSecurityManager.hashCredential():
     * SHA-256, then each byte as two lower-case hex digits.
     */
    private static String hash(String credential) {
        try {
            final byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(credential.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
