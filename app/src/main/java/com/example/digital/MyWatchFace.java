package com.example.digital;

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
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.core.content.ContextCompat;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren"t displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
//Test


public class MyWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. Defaults to one second
     * because the watch face needs to update seconds in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }



        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
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

        private Calendar[] Cals;

        private Calendar mCalendar;
        private Calendar mCalendarPT;
        private Calendar mCalendarCST;
        private Calendar mCalendarSYD;
        private Calendar mCalendarSyD2;

        private Calendar mCalendarLON;


        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Cals[0].setTimeZone(TimeZone.getDefault());
                Cals[1].setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
                Cals[2].setTimeZone(TimeZone.getTimeZone("America/Chicago"));
                Cals[3].setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
                Cals[4].setTimeZone(TimeZone.getTimeZone("Europe/London"));
//                mCalendar.setTimeZone(TimeZone.getDefault());
//                mCalendarPT.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
//                mCalendarCST.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
//                mCalendarSYD.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
//                mCalendarLON.setTimeZone(TimeZone.getTimeZone("Europe/London"));
                invalidate();
            }
        };


        private boolean mRegisteredTimeZoneReceiver = false;
        private float mXOffset;
        private float mYOffset;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mTextPaintZone;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;
        private float batteryPct;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());
            
            mCalendar = Calendar.getInstance();
            mCalendarPT=Calendar.getInstance();
            mCalendarCST=Calendar.getInstance();
            mCalendarSYD=Calendar.getInstance();
            mCalendarLON=Calendar.getInstance();


            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            // Initializes background.
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.background));

            // Initializes Watch Face.
            mTextPaint = new Paint();
            mTextPaint.setTypeface(NORMAL_TYPEFACE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));

            mTextPaintZone = new Paint();
            mTextPaintZone.setTypeface(NORMAL_TYPEFACE);
            mTextPaintZone.setAntiAlias(true);
            mTextPaintZone.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren"t visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                mCalendarPT.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
                mCalendarCST.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
                mCalendarSYD.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
                mCalendarLON.setTimeZone(TimeZone.getTimeZone("Europe/London"));



                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we"re visible (as well as
            // whether we"re in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private float registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return 0;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = MyWatchFace.this.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            batteryPct = level * 100 / (float)scale;
            //System.out.println(batteryPct);
            return batteryPct;
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }



        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float textSizeZone = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round_zone : R.dimen.digital_text_size_zone);

            mTextPaint.setTextSize(textSize);
            mTextPaintZone.setTextSize(textSizeZone);

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;
            if (mLowBitAmbient) {
                mTextPaint.setAntiAlias(!inAmbientMode);
            }

            // Whether the timer should be running depends on whether we"re visible (as well as
            // whether we"re in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mCalendarPT.setTimeInMillis(now);
            mCalendarCST.setTimeInMillis(now);
            mCalendarLON.setTimeInMillis(now);
            mCalendarSYD.setTimeInMillis(now);


            String text0=String.format("Local | "+mCalendar.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT, Locale.ENGLISH)+" "+(mCalendar.getDisplayName(Calendar.MONTH,Calendar.SHORT,Locale.ENGLISH))+" "+mCalendar.get(Calendar.DAY_OF_MONTH));
            String text1 = mAmbient
                    ? String.format("%d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE))
                    : String.format("%d:%02d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE), mCalendar.get(Calendar.SECOND));

            String text2 = mAmbient
                    ? String.format("")
                    : String.format("PT | ")+String.format("%d:%02d", mCalendarPT.get(Calendar.HOUR_OF_DAY),
                    mCalendarPT.get(Calendar.MINUTE));


            String text3 = mAmbient
                    ? String.format("")
                    : String.format("ET | ")+ String.format("%d:%02d", mCalendarPT.get(Calendar.HOUR_OF_DAY)+3,
                    mCalendarPT.get(Calendar.MINUTE));

            String text4 = mAmbient
                    ? String.format("")
                    : String.format("LON | ")+ String.format("%d:%02d", mCalendarLON.get(Calendar.HOUR_OF_DAY),
                    mCalendarLON.get(Calendar.MINUTE));

            String text5 = mAmbient
                    ? String.format("")
                    : String.format("SYD | ")+ String.format("%d:%02d", mCalendarSYD.get(Calendar.HOUR_OF_DAY),
                    mCalendarSYD.get(Calendar.MINUTE));

            String text6 = mAmbient
                    ? String.format("")
                    : String.format("CST | ")+ String.format("%d:%02d", mCalendarCST.get(Calendar.HOUR_OF_DAY),
                    mCalendarCST.get(Calendar.MINUTE));


            canvas.drawText(text0, mXOffset, mYOffset-40, mTextPaintZone);
            canvas.drawText(text1, mXOffset, mYOffset, mTextPaint);
            canvas.drawText(text2, mXOffset, mYOffset+40, mTextPaintZone);
            canvas.drawText(text3, mXOffset, mYOffset+60, mTextPaintZone);
            canvas.drawText(text6, mXOffset, mYOffset+80, mTextPaintZone);
            canvas.drawText(text4, mXOffset, mYOffset+100, mTextPaintZone);
            canvas.drawText(text5, mXOffset, mYOffset+120, mTextPaintZone);


            canvas.drawText(String.valueOf(batteryPct+"%"), mXOffset+60, mYOffset+180, mTextPaintZone);


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn"t currently
         * or stops it if it shouldn"t be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we"re visible and in interactive mode.
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