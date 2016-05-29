/*
 * Copyright (C) 2014 The Android Open Source Project
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

package retro.bailey.rod.retrowatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;



/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class RetroWatchFaceService extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    /**
     * Multiply the width of the square by this scaling factor to get the size of the margin in pixels.
     * The margin is the gap around the outside of all elements, and the vertical gap between the
     * horizontal inner elements.
     */
    private static float SCALE_FACTOR_MARGIN_PX = 0.025F;

    /**
     * Multiply the width of the square by this scaling factor to get the vertical height of the
     * top and bottom elements in pixels.
     */
    private static float SCALE_FACTOR_VERTICAL_TOP_AND_BOTTOM_ELEMENTS_PX = 0.2255F;

    private static float SCALE_FACTOR_VERTICAL_MIDDLE_ELEMENT_PX = 0.4486F;

    private static final String TAG = RetroWatchFaceService.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<RetroWatchFaceService.Engine> mWeakReference;

        public EngineHandler(RetroWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            RetroWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private boolean mRegisteredTimeZoneReceiver = false;
        private Paint mBackgroundPaint;
        private Paint topPanelPaint;
        private Paint middlePanelPaint;
        private Paint bottomPanelPaint;
        private Paint mTextPaint;
        private boolean mAmbient;
        private Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        private int mTapCount;

        private float mXOffset;
        private float mYOffset;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private Typeface peraltaTypeface;

        private int watchFaceHeight;
        private int watchFaceWidth;

        private int marginPx;
        private int shortBarHeightPx;
        private int tallBarHeightPx;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(RetroWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

//            Rect frame = holder.getSurfaceFrame();
//            Log.d(TAG, "frame height=" + frame.height());
//            Log.d(TAG, "frame width=" + frame.width());

            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    Log.d(TAG, "Callback: surfaceCreated: surafaceHolder: width=" + surfaceHolder.getSurfaceFrame().width()
                            + ", height=" + surfaceHolder.getSurfaceFrame().height());
                    watchFaceHeight = (int) surfaceHolder.getSurfaceFrame().height();
                    watchFaceWidth = (int) surfaceHolder.getSurfaceFrame().width();

                    marginPx = (int) (SCALE_FACTOR_MARGIN_PX * watchFaceWidth);
                    shortBarHeightPx = (int) (SCALE_FACTOR_VERTICAL_TOP_AND_BOTTOM_ELEMENTS_PX * watchFaceWidth);
                    tallBarHeightPx = (int) (SCALE_FACTOR_VERTICAL_MIDDLE_ELEMENT_PX * watchFaceWidth);

                    Log.d(TAG, "marginPx=" + marginPx);
                    Log.d(TAG, "shortBarHeightPx=" + shortBarHeightPx);
                    Log.d(TAG, "tallBarHeightPx=" + tallBarHeightPx);
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                    Log.d(TAG, "Callback: surfaceChanged: i=" + i + ", i1=" + i1 + ", i2=" + i2);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    Log.d(TAG, "Callback: surfaceDestroyed");
                }
            });

            Resources resources = RetroWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.text_color));

            topPanelPaint = new Paint();
            topPanelPaint.setColor(resources.getColor(R.color.top_panel_background));

            middlePanelPaint = new Paint();
            middlePanelPaint.setColor(resources.getColor(R.color.middle_panel_background));

            bottomPanelPaint = new Paint();
            bottomPanelPaint.setColor(resources.getColor(R.color.bottom_panel_background));

            peraltaTypeface = Typeface.createFromAsset(getAssets(), "fonts/Ultra.ttf");
            mTextPaint.setTypeface(peraltaTypeface);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            RetroWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            RetroWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = RetroWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = RetroWatchFaceService.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

           // Log.d(TAG, "onDraw: bounds: height=" + bounds.height() + ",width=" + bounds.width());

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                // Draw background
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                // Draw top bar
                Rect topBar = new Rect();
                topBar.set(marginPx, marginPx, watchFaceWidth - marginPx, marginPx + shortBarHeightPx);
                canvas.drawRect(topBar, topPanelPaint);

                // Draw middle bar
                Rect middleBar = new Rect();
                middleBar.set(marginPx, marginPx * 2 + shortBarHeightPx, watchFaceWidth - marginPx, marginPx * 2 + shortBarHeightPx + tallBarHeightPx);
                canvas.drawRect(middleBar, middlePanelPaint);

                // Draw bottom bar
                Rect bottomBar = new Rect();
                bottomBar.set(marginPx, watchFaceHeight - marginPx - shortBarHeightPx, watchFaceWidth - marginPx, watchFaceHeight - marginPx);
                canvas.drawRect(bottomBar, bottomPanelPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d", mTime.hour, mTime.minute);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
