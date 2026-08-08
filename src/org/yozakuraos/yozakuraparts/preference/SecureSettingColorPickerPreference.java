/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.notificationlight.ColorPanelView;
import org.yozakuraos.yozakuraparts.notificationlight.ColorPickerView;
import org.yozakuraos.yozakuraparts.widget.CustomDialogPreference;

import java.util.Locale;

/**
 * YozakuraOS cp116: a colour picker that persists an opaque ARGB int straight into
 * android Settings.Secure under its own key, for consumers that read colours that way
 * (the cutout progress ring, ported in cp114, is the first).
 *
 * Infinity carries its own picker stack (~1900 lines plus resources), but V2 already has
 * one in tree: the notificationlight ColorPickerView / ColorPanelView that cp85 drives
 * through MonetColorPickerPreference. This reuses those widgets and that dialog layout
 * rather than adding a near-duplicate, and reuses the SecureSettingsStore ported in cp115
 * as the data store so persistInt() lands in Settings.Secure.
 *
 * The difference from MonetColorPickerPreference is where the value goes: that one is
 * persistent="false" and hands an RRGGBB hex string to its fragment, because the Monet
 * seed lives inside a JSON blob. Here the key is the setting, so the preference persists
 * itself and no fragment glue is needed.
 *
 * android:defaultValue accepts the 0xAARRGGBB form Infinity's pages use.
 */
public class SecureSettingColorPickerPreference extends CustomDialogPreference<AlertDialog>
        implements ColorPickerView.OnColorChangedListener, TextWatcher {

    private static final int FALLBACK_COLOR = 0xFFFFFFFF;

    private int mColor = FALLBACK_COLOR;
    private int mDefaultColor = FALLBACK_COLOR;

    private ColorPickerView mColorPicker;
    private ColorPanelView mNewColor;
    private EditText mHexInput;

    public SecureSettingColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new SecureSettingsStore(context.getContentResolver()));
        // A small swatch on the right edge previews the current colour.
        setWidgetLayoutResource(R.layout.preference_monet_color_swatch);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // Infinity writes these as android:defaultValue="0xFF2196F3". Depending on how
        // aapt2 encodes it that arrives either as a string or as an already parsed int,
        // so handle both instead of assuming one.
        final String raw = a.getString(index);
        if (raw != null) {
            try {
                return (int) Long.decode(raw.trim()).longValue();
            } catch (NumberFormatException ignored) {
                // fall through to the int form
            }
        }
        return a.getInt(index, FALLBACK_COLOR);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        mDefaultColor = defaultValue instanceof Integer ? (Integer) defaultValue : FALLBACK_COLOR;
        // Show the default without writing it, so an untouched setting stays unset in
        // Settings.Secure and the consumer keeps applying its own default (the same way
        // the seek bars on this page read as null until the user moves them).
        mColor = (restorePersistedValue ? getPersistedInt(mDefaultColor) : mDefaultColor)
                | 0xFF000000;
    }

    @Override
    public CharSequence getSummary() {
        return String.format(Locale.US, "#%06X", mColor & 0x00FFFFFF);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final ImageView swatch = (ImageView) holder.findViewById(R.id.monet_color_swatch);
        if (swatch != null) {
            final int size = (int) getContext().getResources()
                    .getDimension(R.dimen.oval_notification_size);
            // Nudge near-white colours down a touch so the swatch stays visible.
            final int rgb = mColor & 0x00FFFFFF;
            final int shown = ((rgb & 0xF0F0F0) == 0xF0F0F0) ? (rgb - 0x101010) : rgb;
            swatch.setImageDrawable(createOvalShape(size, 0xFF000000 | shown));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_monet_color_picker, null);
        mColorPicker = view.findViewById(R.id.color_picker_view);
        mNewColor = view.findViewById(R.id.color_panel);
        mHexInput = view.findViewById(R.id.hex_color_input);

        mColorPicker.setAlphaSliderVisible(false);
        mColorPicker.setOnColorChangedListener(this);
        // callback=true seeds the panel + hex field via onColorChanged().
        mColorPicker.setColor(mColor | 0xFF000000, true);

        // Null button handlers: they get remapped by CustomPreferenceDialogFragment.
        return new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setView(view)
                .setPositiveButton(R.string.dlg_ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onColorChanged(int color) {
        if (mNewColor != null) {
            mNewColor.setColor(color);
        }
        if (mHexInput != null) {
            mHexInput.removeTextChangedListener(this);
            mHexInput.setText(String.format(Locale.US, "%06x", color & 0x00FFFFFF));
            mHexInput.addTextChangedListener(this);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        final String hex = s.toString();
        if (hex.length() == 6 && mColorPicker != null) {
            try {
                final int color = 0xFF000000 | Color.parseColor('#' + hex);
                mColorPicker.setColor(color, false);
                if (mNewColor != null) {
                    mNewColor.setColor(color);
                }
            } catch (IllegalArgumentException ignored) {
                // Incomplete / invalid hex while typing; ignore.
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mColorPicker != null) {
            mColor = mColorPicker.getColor() | 0xFF000000;
            persistInt(mColor);
            notifyChanged();
        }
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        final ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }
}
