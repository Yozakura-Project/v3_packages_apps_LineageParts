/*
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shows the current wallpaper inside a bento tile, so the wallpaper and lock
 * screen tiles preview what they configure instead of showing an icon.
 *
 * Adapted from Infinity's AnimateWallpaperView. The 60-second Ken Burns pan is
 * deliberately not carried over: it runs a ValueAnimator for as long as the
 * settings screen is open, and the still image reads the same.
 */

package org.yozakuraos.yozakuraparts.yozakura;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;

public class WallpaperPreviewView extends ImageView {

    private static final String TAG = "YozakuraWallpaperPreview";

    /** Wallpapers are far larger than a tile; decode at roughly tile size. */
    private static final int TARGET_PX = 512;

    private boolean mUseLockWallpaper;

    public WallpaperPreviewView(Context context) {
        this(context, null);
    }

    public WallpaperPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.CENTER_CROP);
    }

    /**
     * @param useLock show the lock screen wallpaper, falling back to the system
     *                one when no separate lock wallpaper is set.
     */
    public void setUseLockWallpaper(boolean useLock) {
        mUseLockWallpaper = useLock;
        reload();
    }

    public void reload() {
        final Drawable d = loadWallpaper();
        if (d != null) {
            setImageDrawable(d);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getDrawable() == null) {
            reload();
        }
    }

    private Drawable loadWallpaper() {
        final WallpaperManager wm = getContext().getSystemService(WallpaperManager.class);
        if (wm == null) {
            return null;
        }

        if (mUseLockWallpaper) {
            final Drawable lock = loadLockWallpaper(wm);
            if (lock != null) {
                return lock;
            }
            // No dedicated lock wallpaper: the lock screen shows the system one.
        }

        try {
            return wm.getDrawable();
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read the wallpaper", e);
            return null;
        }
    }

    private Drawable loadLockWallpaper(WallpaperManager wm) {
        ParcelFileDescriptor pfd = null;
        try {
            pfd = wm.getWallpaperFile(WallpaperManager.FLAG_LOCK);
            if (pfd == null) {
                return null;
            }
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, bounds);

            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight);
            final Bitmap bmp =
                    BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, opts);
            return bmp == null ? null : new BitmapDrawable(getResources(), bmp);
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read the lock wallpaper", e);
            return null;
        } finally {
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignored) {
                    // Nothing useful to do; the preview is decorative.
                }
            }
        }
    }

    private static int sampleSizeFor(int width, int height) {
        int sample = 1;
        int longest = Math.max(width, height);
        while (longest / sample > TARGET_PX * 2) {
            sample *= 2;
        }
        return sample;
    }
}
