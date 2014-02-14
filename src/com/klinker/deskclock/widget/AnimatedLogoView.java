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

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.text.ParseException;

public class AnimatedLogoView extends View {
    private static final String TAG = "AnimatedLogoView";

    private static final int TRACE_TIME = 2000;
    private static final int TRACE_TIME_PER_GLYPH = 1000;
    private static final int FILL_START = 1200;
    private static final int FILL_TIME = 2000;
    private static final int MARKER_LENGTH_DIP = 16;
    public static int TRACE_RESIDUE_COLOR = Color.argb(50, 255, 255, 255);
    public static int TRACE_COLOR = Color.WHITE;
    private static final PointF VIEWPORT = new PointF(1000, 300);

    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();

    private Paint mFillPaint;
    private GlyphData[] mGlyphData;
    private float mMarkerLength;
    private int mWidth;
    private int mHeight;
    private long mStartTime;
    private int glyphToUse;

    public static final int STATE_NOT_STARTED = 0;
    public static final int STATE_TRACE_STARTED = 1;
    public static final int STATE_FILL_STARTED = 2;
    public static final int STATE_FINISHED = 3;

    private int mState = STATE_NOT_STARTED;
    private OnStateChangeListener mOnStateChangeListener;

    public AnimatedLogoView(Context context) {
        super(context);
        init();
    }

    public AnimatedLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedLogoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(TRACE_COLOR);

        mMarkerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                MARKER_LENGTH_DIP, getResources().getDisplayMetrics());

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void start() {
        mStartTime = System.currentTimeMillis();
        changeState(STATE_TRACE_STARTED);
        postInvalidate();
    }

    public void reset() {
        mStartTime = 0;
        changeState(STATE_NOT_STARTED);
        postInvalidate();
    }

    public void setToFinishedFrame() {
        mStartTime = 1;
        changeState(STATE_FINISHED);
        postInvalidate();
    }

    public void setGlyphToUse(int glyph) {
        this.glyphToUse = glyph;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        rebuildGlyphData();
    }

    private void rebuildGlyphData() {
        SvgPathParser parser = new SvgPathParser() {
            @Override
            protected float transformX(float x) {
                return x * mWidth / VIEWPORT.x;
            }

            @Override
            protected float transformY(float y) {
                return y * mHeight / VIEWPORT.y;
            }
        };

        mGlyphData = new GlyphData[LogoPaths.GLYPHS.length];
        for (int i = 0; i < LogoPaths.GLYPHS.length; i++) {
            mGlyphData[i] = new GlyphData();
            try {
                mGlyphData[i].path = parser.parsePath(LogoPaths.GLYPHS[i]);
            } catch (ParseException e) {
                mGlyphData[i].path = new Path();
                Log.v(TAG, "Couldn't parse path", e);
            }
            PathMeasure pm = new PathMeasure(mGlyphData[i].path, true);
            while (true) {
                mGlyphData[i].length = Math.max(mGlyphData[i].length, pm.getLength());
                if (!pm.nextContour()) {
                    break;
                }
            }
            mGlyphData[i].paint = new Paint();
            mGlyphData[i].paint.setStyle(Paint.Style.STROKE);
            mGlyphData[i].paint.setAntiAlias(true);
            mGlyphData[i].paint.setColor(Color.WHITE);
            mGlyphData[i].paint.setStrokeWidth(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                            getResources().getDisplayMetrics()));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mState == STATE_NOT_STARTED || mGlyphData == null) {
            return;
        }

        long t = System.currentTimeMillis() - mStartTime;

        // Draw outlines (starts as traced)
        int i = glyphToUse;
        float phase = constrain(0, 1,
                (t - (TRACE_TIME - TRACE_TIME_PER_GLYPH) * i * 1f / mGlyphData.length)
                        * 1f / TRACE_TIME_PER_GLYPH);
        float distance = INTERPOLATOR.getInterpolation(phase) * mGlyphData[i].length;
        mGlyphData[i].paint.setColor(TRACE_RESIDUE_COLOR);
        mGlyphData[i].paint.setPathEffect(new DashPathEffect(
                new float[]{distance, mGlyphData[i].length}, 0));
        canvas.drawPath(mGlyphData[i].path, mGlyphData[i].paint);

        mGlyphData[i].paint.setColor(TRACE_COLOR);
        mGlyphData[i].paint.setPathEffect(new DashPathEffect(
                new float[]{0, distance, phase > 0 ? mMarkerLength : 0,
                        mGlyphData[i].length}, 0));
        canvas.drawPath(mGlyphData[i].path, mGlyphData[i].paint);

        if (t > FILL_START) {
            if (mState < STATE_FILL_STARTED) {
                changeState(STATE_FILL_STARTED);
            }

            // If after fill start, draw fill
            phase = constrain(0, 1, (t - FILL_START) * 1f / FILL_TIME);
            mFillPaint.setAlpha((int) (phase * 255));
                canvas.drawPath(mGlyphData[i].path, mFillPaint);
        }

        if (t < FILL_START + FILL_TIME) {
            // draw next frame if animation isn't finished
            postInvalidate();
        } else {
            changeState(STATE_FINISHED);
        }
    }

    private void changeState(int state) {
        if (mState == state) {
            return;
        }

        mState = state;
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onStateChange(state);
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        mOnStateChangeListener = onStateChangeListener;
    }

    public static interface OnStateChangeListener {
        void onStateChange(int state);
    }

    private static class GlyphData {
        Path path;
        Paint paint;
        float length;
    }

    public static float constrain(float min, float max, float v) {
        return Math.max(min, Math.min(max, v));
    }
}
