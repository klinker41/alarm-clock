/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.deskclock.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import com.klinker.deskclock.R;

public class AnimatedLogoFragment extends Fragment {
    private View mRootView;
    private Runnable mOnFillStartedCallback;
    private AnimatedLogoView mLogoView;
    private float mInitialLogoOffset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInitialLogoOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.animated_logo_fragment, container, false);
        mLogoView = (AnimatedLogoView) mRootView.findViewById(R.id.animated_logo);
        reset();
        return mRootView;
    }

    public void setGlyph(int glyph) {
        mLogoView.setGlyphToUse(glyph);
    }

    public void start() {
        mLogoView.start();
    }

    public void setOnFillStartedCallback(Runnable fillStartedCallback) {
        mOnFillStartedCallback = fillStartedCallback;
    }

    public void reset() {
        mLogoView.reset();
        mLogoView.setTranslationY(mInitialLogoOffset);
    }
}
