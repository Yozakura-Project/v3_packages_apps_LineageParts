/*
 * SPDX-FileCopyrightText: The YozakuraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yozakuraos.yozakuraparts.yozakura;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
 * YozakuraOS cp85: a self-contained ARGB color picker for the Monet engine's
 * custom seed color. Reuses the in-tree ColorPickerView / ColorPanelView widgets
 * (borrowed from the notification-light stack); no external library and no
 * framework change. The chosen color is delivered as an RRGGBB hex string through
 * the standard preference change listener, so YozakuraMonetFragment can write it
 * straight into THEME_CUSTOMIZATION_OVERLAY_PACKAGES.
 */
public class MonetColorPickerPreference extends CustomDialogPreference<AlertDialog>
        implements ColorPickerView.OnColorChangedListener, TextWatcher {

    // Current seed color, RRGGBB (alpha stripped). Default = 夜桜ピンク.
    private int mColor = 0xFF4081;

    private ColorPickerView mColorPicker;
    private ColorPanelView mNewColor;
    private EditText mHexInput;

    public MonetColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // A small swatch on the right edge previews the current seed color.
        setWidgetLayoutResource(R.layout.preference_monet_color_swatch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final ImageView swatch =
                (ImageView) holder.findViewById(R.id.monet_color_swatch);
        if (swatch != null) {
            final int size = (int) getContext().getResources()
                    .getDimension(R.dimen.oval_notification_size);
            // Nudge near-white colors down a touch so the swatch stays visible.
            final int rgb = mColor & 0x00FFFFFF;
            final int shown = ((rgb & 0xF0F0F0) == 0xF0F0F0) ? (rgb - 0x101010) : rgb;
            swatch.setImageDrawable(createOvalShape(size, 0xFF000000 | shown));
        }
    }

    /** RRGGBB hex string (no leading '#', lowercase). */
    public String getColorHex() {
        return String.format(Locale.US, "%06x", mColor & 0x00FFFFFF);
    }

    /** Set the current color from an RRGGBB int (alpha ignored). */
    public void setColorRgb(int rgb) {
        mColor = rgb & 0x00FFFFFF;
        notifyChanged();
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
        mColorPicker.setColor(0xFF000000 | mColor, true);

        // Null button handlers: they get remapped by CustomPreferenceDialogFragment.
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.yozakura_monet_custom_color_title)
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

    // --- TextWatcher on the hex field: type a hex -> move the picker (no callback,
    //     so we don't loop back into onColorChanged). ---
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
            mColor = mColorPicker.getColor() & 0x00FFFFFF;
            notifyChanged();
            callChangeListener(getColorHex());
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
