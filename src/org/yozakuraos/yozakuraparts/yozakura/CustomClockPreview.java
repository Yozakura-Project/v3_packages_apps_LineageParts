/*
 * Copyright (C) 2023-2024 The risingOS Android Project
 * Copyright (C) 2024-25 Project Infinity X 
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
package org.yozakuraos.yozakuraparts.yozakura;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.yozakuraos.yozakuraparts.R;
import org.yozakuraos.yozakuraparts.SettingsPreferenceFragment;

import org.yozakuraos.yozakuraparts.YozakuraUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class CustomClockPreview extends SettingsPreferenceFragment {

    private static final String TAG = "LockClockPreview";
    private static final String PREF_FIRST_TIME = "first_time_clock_face_access";

    private ViewPager viewPager;
    private ClockPagerAdapter pagerAdapter;
    private ExtendedFloatingActionButton applyFab;
    private View highlightGuide;
    private TextView clockNameTextView;

    private int mClockPosition = 0;



    private static final int[] CLOCK_LAYOUTS = {
            R.layout.keyguard_clock_default,
            R.layout.keyguard_clock_empty,
            R.layout.keyguard_clock_oos,
            R.layout.keyguard_clock_oos2,
            R.layout.keyguard_clock_center,
            R.layout.keyguard_clock_ios,
            R.layout.keyguard_clock_ios2,
            R.layout.keyguard_clock_ios3,
            R.layout.keyguard_clock_ios4,
            R.layout.keyguard_clock_ios5,
            R.layout.keyguard_clock_ios6,
            R.layout.keyguard_clock_ios7,
            R.layout.keyguard_clock_ios8,
            R.layout.keyguard_clock_ios9,
            R.layout.keyguard_clock_ios10,
            R.layout.keyguard_clock_ios11,
            R.layout.keyguard_clock_ios12,
            R.layout.keyguard_clock_ios13,
            R.layout.keyguard_clock_ios14,
            R.layout.keyguard_clock_ios15,
            R.layout.keyguard_clock_ios16,
            R.layout.keyguard_clock_ios17,
            R.layout.keyguard_clock_ios18,
            R.layout.keyguard_clock_ios19,
            R.layout.keyguard_clock_miui,
            R.layout.keyguard_clock_miui2,
            R.layout.keyguard_clock_cos1,
            R.layout.keyguard_clock_cos2,
            R.layout.keyguard_clock_simple,
            R.layout.keyguard_clock_ide,
            R.layout.keyguard_clock_moto,
            R.layout.keyguard_clock_stylish,
            R.layout.keyguard_clock_stylish2,
            R.layout.keyguard_clock_stylish3,
            R.layout.keyguard_clock_stylish4,
            R.layout.keyguard_clock_stylish5,
            R.layout.keyguard_clock_stylish6,
            R.layout.keyguard_clock_stylish7,
            R.layout.keyguard_clock_stylish8,
            R.layout.keyguard_clock_stylish9,
            R.layout.keyguard_clock_stylish10,
            R.layout.keyguard_clock_word,
            R.layout.keyguard_clock_life,
            R.layout.keyguard_clock_a9,
            R.layout.keyguard_clock_nos1,
            R.layout.keyguard_clock_nos2,
            R.layout.keyguard_clock_num,
            R.layout.keyguard_clock_accent,
            R.layout.keyguard_clock_analog,
            R.layout.keyguard_clock_block,
            R.layout.keyguard_clock_bubble,
            R.layout.keyguard_anci_clock_outline,
            R.layout.keyguard_anci_clock_ovalium,
            R.layout.keyguard_anci_clock_rectangle,
            R.layout.keyguard_anci_clock_wallet,
            R.layout.keyguard_anci_clockdate_clavicula,
            R.layout.keyguard_anci_clockdate_kln,
            R.layout.keyguard_anci_clockdate_miring,
            R.layout.keyguard_anci_clockdate_scapula,
            R.layout.keyguard_anci_clockdate_sternum,
            R.layout.keyguard_sparkCircle,
            R.layout.keyguard_sparkList,
            R.layout.keyguard_clock_big1,
            R.layout.keyguard_clock_big2,
            R.layout.keyguard_clock_big3,
            R.layout.keyguard_clock_big4,
            R.layout.keyguard_clock_sweet,
            R.layout.keyguard_clock_pixel,
            R.layout.keyguard_clock_samurai,
            R.layout.keyguard_clock_gateway,
            R.layout.keyguard_clock_tall,
            R.layout.keyguard_clock_gobold,
            R.layout.keyguard_clock_gobold2,
            R.layout.keyguard_clock_delirium,
            R.layout.keyguard_clock_deliriumdual,
            R.layout.keyguard_clock_skewrom,
            R.layout.keyguard_clock_skewrom2,
            R.layout.keyguard_clock_taller,
            R.layout.keyguard_clock_taller2,
            R.layout.keyguard_clock_taller3,
            R.layout.keyguard_clock_modak,
            R.layout.keyguard_clock_galada,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getActivity().getString(R.string.custom_clock_style));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final android.content.Context themedContext =
                new android.view.ContextThemeWrapper(getActivity(), R.style.YozakuraClockPickerTheme);
        View rootView = inflater.cloneInContext(themedContext)
                .inflate(R.layout.lockscreen_clock_preview, container, false);
        clockNameTextView = rootView.findViewById(R.id.clock_name);

        viewPager = rootView.findViewById(R.id.view_pager);
        pagerAdapter = new ClockPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        mClockPosition = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                "lock_screen_custom_clock_style", 0, UserHandle.USER_CURRENT);
        if (mClockPosition < 0 || mClockPosition >= CLOCK_LAYOUTS.length) {
            mClockPosition = 0;
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    "lock_screen_custom_clock_style", 0, UserHandle.USER_CURRENT);
        }
        viewPager.setCurrentItem(mClockPosition);

        applyFab = rootView.findViewById(R.id.apply_extended_fab);
        applyFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.Secure.putIntForUser(getContext().getContentResolver(),
                        "lock_screen_custom_clock_style", mClockPosition,
                        UserHandle.USER_CURRENT);
                Settings.Secure.putIntForUser(getContext().getContentResolver(),
                        "lock_screen_custom_clock_face", 0,
                        UserHandle.USER_CURRENT);
                YozakuraUtils.restartSystemUI();
            }
        });

        highlightGuide = rootView.findViewById(R.id.highlight_guide);
        if (isFirstTime()) {
            highlightGuide.setVisibility(View.VISIBLE);
            highlightGuide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    highlightGuide.setVisibility(View.GONE);
                    disableHighlight();
                }
            });
        } else {
            highlightGuide.setVisibility(View.GONE);
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                mClockPosition = position;
                if (viewPager != null) {
                    viewPager.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                }
                updateClockName(position);
            }
        });
        return rootView;
    }
    
    private void updateClockName(int position) {
        String[] clockNames = {
            "Default Clock",
            "No Clock",
            "OnePlus Clock",
            "OnePlus Clock 2",
            "iOS Clock Legacy",
            "iOS Clock",
            "iOS Clock 2",
            "iOS Clock 3",
            "iOS Clock 4",
            "iOS Clock 5",
            "iOS Clock 6",
            "iOS Clock 7",
            "iOS Clock 8",
            "iOS Clock 9",
            "iOS Clock 10",
            "iOS Clock 11",
            "iOS Clock 12",
            "iOS Clock 13",
            "iOS Clock 14",
            "iOS Clock 15",
            "iOS Clock 16",
            "iOS Clock 17",
            "iOS Clock 18",
            "iOS Clock 19",
            "MIUI Clock",
            "HyperOS Clock",
            "ColorOS Clock 1",
            "ColorOS Clock 2",
            "Simple Clock",
            "IDE Clock",
            "Moto Clock",
            "Stylish Clock",
            "Stylish Clock 2",
            "Stylish Clock 3",
            "Stylish Clock 4",
            "Stylish Clock 5",
            "Stylish Clock 6",
            "Stylish Clock 7",
            "Stylish Clock 8",
            "Stylish Clock 9",
            "Stylish Clock 10",
            "Text Clock",
            "LifeStyle Clock",
            "Android 9 Vibe",
            "NothingOS 1 Clock",
            "NothingOS 2 Clock",
            "Stacked Clock",
            "X Factor",
            "Simple Analog",
            "Block",
            "Bubble",
            "Outline",
            "Ovalium",
            "Rectangle",
            "Wallet",
            "Clavicula",
            "KLN",
            "Miring",
            "Scapula",
            "Sternum",
            "Circle",
            "List",
            "Big Clock 1",
            "Big Clock 2",
            "Big Clock 3",
            "Big Clock 4",
            "Sweet",
            "Pixel",
            "Samurai",
            "Gateway",
            "Tall Clock",
            "GoBold monet",
            "GoBold",
            "Delirium",
            "Delirium Dual",
            "Skewrom",
            "Skewrom 2",
            "Taller",
            "Taller 2",
            "Taller 3",
            "Modak",
            "Galada"
        };
        if (clockNameTextView != null && position >= 0 && position < clockNames.length) {
            clockNameTextView.setText(clockNames[position]);
        }
    }


    
    private boolean shouldScaleDown(int position) {
        int layoutId = CLOCK_LAYOUTS[position];
        return layoutId == R.layout.keyguard_clock_stylish
               || layoutId == R.layout.keyguard_clock_stylish2 || layoutId == R.layout.keyguard_clock_stylish3
               || layoutId == R.layout.keyguard_clock_stylish4 || layoutId == R.layout.keyguard_clock_stylish5
               || layoutId == R.layout.keyguard_clock_stylish6 || layoutId == R.layout.keyguard_clock_stylish7
               || layoutId == R.layout.keyguard_clock_stylish8 || layoutId == R.layout.keyguard_clock_stylish9
               || layoutId == R.layout.keyguard_clock_stylish10;
    }

    private boolean isFirstTime() {
        return Settings.System.getIntForUser(
            getContext().getContentResolver(), PREF_FIRST_TIME, 1, UserHandle.USER_CURRENT) != 0;
    }

    private void disableHighlight() {
        Settings.System.putIntForUser(getContext().getContentResolver(), PREF_FIRST_TIME, 0, UserHandle.USER_CURRENT);
    }

    private class ClockPagerAdapter extends PagerAdapter {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View layout = inflater.inflate(CLOCK_LAYOUTS[position], container, false);

            int bottomPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                150, 
                getResources().getDisplayMetrics()
            );
            layout.setPadding(
                layout.getPaddingLeft(), 
                layout.getPaddingTop(), 
                layout.getPaddingRight(), 
                bottomPadding
            );
            
            if (shouldScaleDown(position)) {
                layout.setScaleX(0.5f);
                layout.setScaleY(0.5f);
            }
            
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return CLOCK_LAYOUTS.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateClockName(mClockPosition);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateClockName(mClockPosition);
    }

}
