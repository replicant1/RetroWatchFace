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
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import retro.bailey.rod.retrowatchface.config.Theme;
import retro.bailey.rod.retrowatchface.config.Themes;


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
        private Paint backgroundPaint;
        private Paint dayNameBackgroundPaint;
        private Paint timeBackgroundPaint;
        private Paint dateBackgroundPaint;
        private Paint timeTextPaint;
        private Paint dayNameTextPaint;
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

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean lowBitAmbientModeSupported;

        private int watchFaceHeight;
        private int watchFaceWidth;

        private int marginPx;
        private int shortBarHeightPx;
        private int tallBarHeightPx;
        private Paint dateTextPaint;

        // Current theme for watch face (colors and fonts)
        private Theme theme;

        /**
         * Invoked whenever the theme of the watch face is changed, either by the user or the system.
         *
         * @param newTheme The new theme to be adopted.
         */
        private void onThemeChange(Theme newTheme) {
            theme = newTheme;

            Resources resources = RetroWatchFaceService.this.getResources();

            // Full background is filled with this color as first step of drawing. WHen done, this
            // color only shows through in the margins between the bars.
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.parseColor(theme.backgroundColor));

            // Top most bar contains the day name e.g. "Tuesday"
            dayNameBackgroundPaint = new Paint();
            dayNameBackgroundPaint.setColor(Color.parseColor(theme.day.backgroundColor));
            dayNameTextPaint = createTextPaint(Color.parseColor(theme.day.textColor), theme.day.textFont, theme.day.textSize);

            // Middle bar contains the time e.g. "12:33"
            timeBackgroundPaint = new Paint();
            timeBackgroundPaint.setColor(Color.parseColor(theme.time.backgroundColor));
            timeTextPaint = createTextPaint(Color.parseColor(theme.time.textColor), theme.time.textFont, theme.time.textSize);

            // Bottom bar contains the date e.g. "20 February"
            dateBackgroundPaint = new Paint();
            dateBackgroundPaint.setColor(Color.parseColor(theme.date.backgroundColor));
            dateTextPaint = createTextPaint(Color.parseColor(theme.date.textColor), theme.date.textFont, theme.date.textSize);
        }

        private Theme initThemes() {
            BufferedReader reader = null;
            StringBuffer buffer = new StringBuffer();

            try {
                reader = new BufferedReader(new InputStreamReader(getAssets().open("themes.json")));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            } catch (IOException iox) {
                Log.w(TAG, iox);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }
            }

            String jsonString = buffer.toString();

            // By the time we get here, 'buffer' contains the JSON string from assets/themes.json
            Log.i(TAG, "Read themes.json: " + jsonString);

            // Parse the JSON string into an object graph
            Gson gson = new Gson();
            Themes themes = gson.fromJson(jsonString, Themes.class);

            Log.i(TAG, "As read in from theme.json, object graph is:" + themes.toString());

            // Return the initially selected theme
            return themes.themes.get(0); // 0 = Marine
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            onThemeChange(initThemes());

            setWatchFaceStyle(new WatchFaceStyle.Builder(RetroWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

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

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, String textFont, String textSize) {
            Paint paint = new Paint();

            paint.setColor(textColor);
            paint.setAntiAlias(true);
            paint.setTypeface(Typeface.createFromAsset(getAssets(), textFont));

            String textSizeStr = textSize.substring(0, textSize.length() - 2);
            Log.d(TAG, "textSize=" + textSize + ", textSizeStr=" + textSizeStr);

            paint.setTextSize(Float.parseFloat(textSizeStr));

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

            // Adjust text size for time
            float timeTextSize = resources.getDimension(isRound
                    ? R.dimen.time_text_size_round : R.dimen.time_text_size_square);
            timeTextPaint.setTextSize(timeTextSize);

            // Adjust text size for day name
            float dayNameTextSize = resources.getDimension(isRound
                    ? R.dimen.day_name_text_size_round : R.dimen.day_name_text_size_square);
            dayNameTextPaint.setTextSize(dayNameTextSize);

            // Adjust text size for date
            float dateTextSize = resources.getDimension(isRound ? R.dimen.date_text_size_round : R.dimen.date_text_size_square);
            dateTextPaint.setTextSize(dateTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbientModeSupported = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
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
                if (lowBitAmbientModeSupported) {
                    timeTextPaint.setAntiAlias(!inAmbientMode);
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
                    backgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // Log.d(TAG, "onDraw: bounds: height=" + bounds.height() + ",width=" + bounds.width());
            mTime.setToNow();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                drawBars(canvas, bounds);
                drawTimeInMiddleBar(canvas);
                drawDayInTopBar(canvas);
                drawDateInBottomBar(canvas);
            }
        }

        private void drawBars(Canvas canvas, Rect bounds) {
            // Draw background
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroundPaint);

            // Draw top bar that contains the name of the day of week
            Rect topBar = new Rect();
            topBar.set(marginPx, marginPx, watchFaceWidth - marginPx, marginPx + shortBarHeightPx);
            canvas.drawRect(topBar, dayNameBackgroundPaint);

            // Draw middle bar that contains the time
            Rect middleBar = new Rect();
            middleBar.set(marginPx, marginPx * 2 + shortBarHeightPx, watchFaceWidth - marginPx, marginPx * 2 + shortBarHeightPx + tallBarHeightPx);
            canvas.drawRect(middleBar, timeBackgroundPaint);

            // Draw bottom bar that contains the date
            Rect bottomBar = new Rect();
            bottomBar.set(marginPx, watchFaceHeight - marginPx - shortBarHeightPx, watchFaceWidth - marginPx, watchFaceHeight - marginPx);
            canvas.drawRect(bottomBar, dateBackgroundPaint);
        }

        /**
         * Draws the full name of the current day of the week (eg. "Monday") in the center of the
         * top bar.
         *
         * @param canvas
         */
        private void drawDayInTopBar(Canvas canvas) {
            Calendar cal = Calendar.getInstance();
            String dayName = mTime.format("%A");
            Log.d(TAG, "dayName=" + dayName);

            Paint.FontMetricsInt fontMetricsInt = dayNameTextPaint.getFontMetricsInt();
            int dayNameHeightPx = fontMetricsInt.ascent * -1;
            int dayNameWidthPx = (int) dayNameTextPaint.measureText(dayName);

            float centerX = watchFaceWidth / 2.0F;
            float centerDayNameY = marginPx + (shortBarHeightPx / 2);
            dayNameTextPaint.setTextAlign(Paint.Align.CENTER);

          /*  canvas.drawRect(
                    centerX - (dayNameWidthPx /2), // left
                    centerDayNameY - (dayNameHeightPx / 2), // top
                    centerX + (dayNameWidthPx / 2), // right
                    centerDayNameY + (dayNameHeightPx / 2), // bottom
                    middlePanelPaint);*/

            canvas.drawText(
                    dayName, // "Monday"
                    centerX, //
                    centerDayNameY - ((fontMetricsInt.ascent + fontMetricsInt.descent) / 2), //
                    dayNameTextPaint);

        }

        /**
         * Draws the current time in the middle bar running horizontally across the middle of the screen.
         *
         * @param canvas
         */
        private void drawTimeInMiddleBar(Canvas canvas) {
            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            // TODO: 24 hour or am/pm?

            String timeText = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d", mTime.hour, mTime.minute);

            Paint.FontMetricsInt textFontMetricsInt = timeTextPaint.getFontMetricsInt();
            int timeHeightPx = textFontMetricsInt.ascent * -1;
            int timeWidthPx = (int) timeTextPaint.measureText(timeText);

            float centerX = watchFaceWidth / 2.0F;
            float centerY = watchFaceHeight / 2.0F;
            timeTextPaint.setTextAlign(Paint.Align.CENTER);

            /*
            canvas.drawRect(
                    centerX - (timeWidthPx / 2), // left
                    centerY - (timeHeightPx / 2), // top
                    centerX + (timeWidthPx / 2), // right
                    centerY + (timeHeightPx / 2), // bottom
                    topPanelPaint); */

            canvas.drawText(
                    timeText, // "12:37"
                    centerX,
                    centerY - ((textFontMetricsInt.ascent + textFontMetricsInt.descent) / 2),
                    timeTextPaint);

            Log.d(TAG, String.format("timeHeightPx= %d, timeWidthPx = %d, centreX = %f, centreY = %f, ascent = %d, descent = %d",
                    timeHeightPx, timeWidthPx, centerX, centerY, textFontMetricsInt.ascent, textFontMetricsInt.descent));
        }

        /**
         * Draws the current date in the bottom bar running horizontally across the bottoom of the
         * screen. eg. "28 May 2016"
         *
         * @param canvas
         */
        private void drawDateInBottomBar(Canvas canvas) {
            String dateStr = mTime.format("%e %B").trim();
            Log.d(TAG, "dateStr=" + dateStr);

            Paint.FontMetricsInt textFontMetricsInt = dateTextPaint.getFontMetricsInt();
            int dateHeightPx = textFontMetricsInt.ascent * -1;
            int dateWidthPx = (int) dateTextPaint.measureText(dateStr);

            Log.d(TAG, "dateWidth=" + dateWidthPx);

            float centerX = watchFaceWidth / 2.0F;
            float centerYOfBottomBar = watchFaceHeight - marginPx - (shortBarHeightPx / 2);

            dateTextPaint.setTextAlign(Paint.Align.CENTER);

          /*  canvas.drawRect(
                    centerX - (dateWidthPx / 2), // left
                    centerYOfBottomBar- (dateHeightPx / 2), // top
                    centerX + (dateWidthPx / 2), // right
                    centerYOfBottomBar + (dateHeightPx / 2), // bottom
                    middlePanelPaint);*/

            canvas.drawText(
                    dateStr, // "12 JUN 2016"
                    centerX,
                    centerYOfBottomBar - ((textFontMetricsInt.ascent + textFontMetricsInt.descent) / 2),
                    dateTextPaint);
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
