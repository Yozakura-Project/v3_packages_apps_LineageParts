/*
 * Copyright (C) 2021 Project Radiant
 * SPDX-FileCopyrightText: The YozakuraOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yozakuraos.yozakuraparts;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class WallpaperView extends ImageView {

    private final Context contextM;

    public WallpaperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        contextM = context;
    }

    public WallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        contextM = context;
    }

    public WallpaperView(Context context) {
        super(context);
        contextM = context;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(contextM);
        Drawable wallpaperDrawable = null;
        try {
            wallpaperDrawable = wallpaperManager.getDrawable();
        } catch (Exception e) {
            // Missing permission or no wallpaper; fall back to no preview background.
        }

        if (wallpaperDrawable != null) {
            ColorDrawable dimOverlay = new ColorDrawable(Color.argb(51, 0, 0, 0));
            Drawable[] layers = new Drawable[]{wallpaperDrawable, dimOverlay};
            setImageDrawable(new LayerDrawable(layers));
        } else {
            setImageDrawable(null);
        }
    }
}
