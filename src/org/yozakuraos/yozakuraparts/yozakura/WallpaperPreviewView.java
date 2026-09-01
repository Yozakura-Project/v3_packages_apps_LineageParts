/*
 * SPDX-FileCopyrightText: 2024 Infinity X
 * SPDX-FileCopyrightText: 2026 YozakuraOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported from Infinity's AnimateWallpaperView: shows the current wallpaper in a
 * bento tile, zoomed 1.5x and panned along a slow sin/cos path over 60 seconds,
 * dimmed so a label stays readable on top of it.
 *
 * Changes from the original, all of them guards rather than behaviour:
 *   - getDrawable() is not blind-cast to BitmapDrawable. It returns a
 *     ColorDrawable for a solid-colour wallpaper and can return null, either of
 *     which crashed the original; anything that is not a bitmap is rasterised.
 *   - the viewport is clamped with the width held constant. The original
 *     recomputed right from width() after mutating left, so the viewport
 *     silently changed size at the edges of the pan.
 *   - the animation stops while the window is not visible, not only on detach.
 *   - optionally shows the lock wallpaper.
 */

package org.yozakuraos.yozakuraparts.yozakura;

import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.io.IOException;

public class WallpaperPreviewView extends ImageView {

    private static final String TAG = "YozakuraWallpaperPreview";

    private static final long ANIMATION_DURATION_MS = 60000L;
    private static final float ZOOM_FACTOR = 1.5f;
    private static final float DIM_ALPHA = 0.1f;

    private final Paint mPaint = new Paint();
    private final Paint mDimPaint = new Paint();
    private final RectF mViewport = new RectF(0, 0, 100, 100);
    private final Rect mSrc = new Rect();
    private final Rect mDst = new Rect();

    private Bitmap mWallpaper;
    private ValueAnimator mAnimator;
    private boolean mUseLockWallpaper;

    public WallpaperPreviewView(Context context) {
        this(context, null);
    }

    public WallpaperPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mDimPaint.setAntiAlias(true);
        mDimPaint.setColor(Color.argb((int) (DIM_ALPHA * 255), 0, 0, 0));
    }

    /**
     * @param useLock show the lock screen wallpaper, falling back to the home
     *                one when no separate lock wallpaper is set.
     */
    public void setUseLockWallpaper(boolean useLock) {
        if (mUseLockWallpaper == useLock) {
            return;
        }
        mUseLockWallpaper = useLock;
        mWallpaper = null;
        if (getWidth() > 0 && getHeight() > 0) {
            load();
            startAnimation();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        load();
        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mWallpaper != null && !mWallpaper.isRecycled()) {
            mSrc.set((int) mViewport.left, (int) mViewport.top,
                    (int) mViewport.right, (int) mViewport.bottom);
            mDst.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(mWallpaper, mSrc, mDst, mPaint);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), mDimPaint);
    }

    private void load() {
        if (mWallpaper != null) {
            return;
        }
        final WallpaperManager wm = getContext().getSystemService(WallpaperManager.class);
        if (wm == null) {
            return;
        }

        if (mUseLockWallpaper) {
            mWallpaper = loadLockWallpaper(wm);
        }
        if (mWallpaper == null) {
            try {
                mWallpaper = toBitmap(wm.getDrawable());
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to read the wallpaper", e);
            }
        }
        if (mWallpaper != null) {
            resetViewport();
        }
    }

    private Bitmap loadLockWallpaper(WallpaperManager wm) {
        ParcelFileDescriptor pfd = null;
        try {
            pfd = wm.getWallpaperFile(WallpaperManager.FLAG_LOCK);
            if (pfd == null) {
                // No dedicated lock wallpaper: the lock screen shows the home one.
                return null;
            }
            return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read the lock wallpaper", e);
            return null;
        } finally {
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignored) {
                    // The preview is decorative; nothing useful to do here.
                }
            }
        }
    }

    /**
     * A wallpaper is not necessarily a bitmap: a solid colour comes back as a
     * ColorDrawable, and the original crashed on the cast.
     */
    private Bitmap toBitmap(Drawable d) {
        if (d == null) {
            return null;
        }
        if (d instanceof BitmapDrawable) {
            final Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b != null) {
                return b;
            }
        }
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            w = Math.max(getWidth(), 1);
            h = Math.max(getHeight(), 1);
        }
        final Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(out);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return out;
    }

    private void resetViewport() {
        mViewport.set(0, 0,
                mWallpaper.getWidth() / ZOOM_FACTOR,
                mWallpaper.getHeight() / ZOOM_FACTOR);
    }

    /**
     * Honour the system animation setting. With animations off the pan is just
     * battery use the user has already said they do not want; the still frame
     * reads the same.
     */
    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    private void startAnimation() {
        stopAnimation();
        if (mWallpaper == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        if (!animationsEnabled()) {
            resetViewport();
            invalidate();
            return;
        }

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(ANIMATION_DURATION_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(a -> {
            final float fraction = a.getAnimatedFraction();

            final float halfW = getWidth() / (2 * ZOOM_FACTOR);
            final float halfH = getHeight() / (2 * ZOOM_FACTOR);
            final float centerX = mWallpaper.getWidth() / 2f;
            final float centerY = mWallpaper.getHeight() / 2f;
            final float maxDx = (mWallpaper.getWidth() - getWidth() / ZOOM_FACTOR) / 2f;
            final float maxDy = (mWallpaper.getHeight() - getHeight() / ZOOM_FACTOR) / 2f;

            final float dx = maxDx * (float) Math.sin(fraction * Math.PI * 2);
            final float dy = maxDy * (float) Math.cos(fraction * Math.PI * 2);

            // Clamp with the viewport size held constant, so panning to an edge
            // does not quietly change how much of the wallpaper is shown.
            final float vw = halfW * 2;
            final float vh = halfH * 2;
            float left = centerX - dx - halfW;
            float top = centerY - dy - halfH;
            left = Math.max(0, Math.min(mWallpaper.getWidth() - vw, left));
            top = Math.max(0, Math.min(mWallpaper.getHeight() - vh, top));
            mViewport.set(left, top, left + vw, top + vh);

            invalidate();
        });
        mAnimator.start();
    }

    private void stopAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator.removeAllUpdateListeners();
            mAnimator = null;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            load();
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
