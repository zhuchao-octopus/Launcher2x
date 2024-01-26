package com.android.launcher2;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import com.android.launcher.R;
import com.common.util.BroadcastUtil;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.AppConfig;
import com.common.util.ProtocolAk47;
import com.common.util.SystemConfig;
import com.my.radio.MarkFaceView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.provider.Settings;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.common.util.UtilCarKey;
import com.common.view.MyScrollView;
import com.common.view.MyScrollView.ICallBack;

public class RadioMusicWidgetView {
    public static Launcher mContext;
    @SuppressLint("StaticFieldLeak")
    private static RadioMusicWidgetView mThis;

    TextView mClock;
    TextView mDate;
    TextView mDate2;

    TextView mCEDate;
    TextView mRadioBaud;
    TextView mRadioFreq;
    TextView mRadioHz;

    // View mMusicIcon;
    TextView mMusicName;
    TextView mMusicTime;
    TextView mMusicTime2;
    ImageView mMusicArt;

    SeekBar mMusicSeekBar;

    int mMusicCurTime;
    int mMusicTotalTime;
    String mName;

    MarkFaceView mMarkFace;

    private final static String TAG = "RadioMusicWidgetView";
    private final static String TAG2 = "RadioMusicWidgetView2";

    public static RadioMusicWidgetView getInstance(Launcher ac) {
        // if (mThis == null) {
        Log.d(TAG, mThis + ":getInstance:" + ac.findViewById(R.id.entry_time));
        if (ac.findViewById(R.id.entry_time) != null) {
            mThis = new RadioMusicWidgetView();
            mThis.init(ac);
        }
        // }
        // mThis.updateRadioInfo();
        return mThis;
    }

    private boolean mPause = true;

    public static void onPause() {

        if (mThis != null) {
            mThis.doPause();
            mThis.mPause = true;
        }
    }

    public static void onResume() {
        if (mThis != null) {
            mThis.mPause = false;
            mThis.doResume();

        }

    }

    public static void onDestroy() {
        if (mThis != null) {
            mThis.unregisterReceiver();
            mThis.mPause = true;
        }
        mThis = null;
    }

    private void doPause() {
        // mSource = MyCmd.SOURCE_NONE;
        mHandler.removeMessages(0);
        // unregisterReceiver();

    }

    private void doResume() {
        // mSource = MyCmd.SOURCE_NONE;
        registerReceiver();

        BroadcastUtil.sendToCarService(mContext,
                MyCmd.Cmd.QUERY_CURRENT_SOURCE, 0);

        updateTime();
        initMoreInfo();
        flashTime(true);
        // updateRadio();

    }

    private View mBTMusicLayout = null;
    private boolean mFlashTime = false;
    private boolean mFlashTimeShow = true;

    public void init(Launcher ac) {
        mContext = ac;
        mPause = false;

        if (MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(Utilities.mSystemUI)) {
            mFlashTime = true;
        }

        if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(Utilities.mSystemUI)
                || MachineConfig.VALUE_SYSTEM_UI21_RM10_2
                .equals(Utilities.mSystemUI)
                || MachineConfig.VALUE_SYSTEM_UI21_RM12
                .equals(Utilities.mSystemUI)
                || MachineConfig.VALUE_SYSTEM_UI45_8702_2
                .equals(Utilities.mSystemUI)
                || MachineConfig.VALUE_SYSTEM_UI_PX30_1
                .equals(Utilities.mSystemUI)) {
            mFMNum = 3;
        }

        mClock = (TextView) mContext.findViewById(R.id.radio_music_time);

        mBTMusicLayout = mContext.findViewById(R.id.entry_bt_music);

        // mClock.setTypeface(fromAsset);

        mDate = (TextView) mContext.findViewById(R.id.radio_music_date);
        mCEDate = (TextView) mContext.findViewById(R.id.ce_date);

        mDate2 = (TextView) mContext.findViewById(R.id.radio_music_date2);
        mRadioBaud = (TextView) mContext.findViewById(R.id.radio_music_baud);
        mRadioFreq = (TextView) mContext.findViewById(R.id.radio_music_freq);

        mRadioHz = (TextView) mContext.findViewById(R.id.radio_music_hz);

        mViewHour = mContext.findViewById(R.id.clock_hour);
        mViewMinutus = mContext.findViewById(R.id.clock_minute);
        mViewSec = mContext.findViewById(R.id.clock_sec);

        mSpeedPoint = mContext.findViewById(R.id.speed_point);
        mBearingPoint = mContext.findViewById(R.id.bearing_point);
        mTvSpeed = (TextView) mContext.findViewById(R.id.speed_text);

        mTvAtitude = (TextView) mContext.findViewById(R.id.atitude_text);

        // mMusicIcon = mContext.findViewById(R.id.music_icon);
        mMusicName = (TextView) mContext.findViewById(R.id.music_name);
        mMusicTime = (TextView) mContext.findViewById(R.id.music_time);
        mMusicTime2 = (TextView) mContext.findViewById(R.id.music_time2);
        View v = mContext.findViewById(R.id.music_progress);
        if (v != null) {
            mMusicSeekBar = (SeekBar) v;
        }
        mMusicArt = (ImageView) mContext.findViewById(R.id.album_art);
        mMarkFace = (MarkFaceView) mContext.findViewById(R.id.radio_mark_face_view);

        /// TextView mRadioBaud;
        /// TextView mRadioFreq;
        /// TextView mRadioHz;

        String value = Utilities.mSystemUI;
        // if (MachineConfig.VALUE_SYSTEM_UI_KLD3_8702.equals(value)) {
        // Typeface fromAsset = Typeface
        // .createFromFile("/system/fonts/AndroidClock_Highlight.ttf");
        // fromAsset = Typeface.create(fromAsset, Typeface.BOLD);
        // mClock.setTypeface(fromAsset);
        // mRadioFreq.setTypeface(fromAsset);
        // }
        if (MachineConfig.VALUE_SYSTEM_UI_KLD3.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI_KLD10_887.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI16_7099.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI22_1050.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI35_KLD813_2.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI42_913.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI_887_90.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(value)) {

        } else {
            AssetManager assets = ac.getAssets();

            Typeface fromAsset = Typeface.createFromAsset(assets,
                    "fonts/DS-DIGIT.TTF");
            fromAsset = Typeface.create(fromAsset, Typeface.ITALIC);
            mRadioFreq.setTypeface(fromAsset);
        }

        mMyScrollView = (MyScrollView) mContext.findViewById(R.id.layout_mid);
        if (mMyScrollView != null) {
            mMyScrollView.setCallback(mICallBack);
            v = mContext.findViewById(R.id.mid_page1);
            if (v != null) {
                v.getBackground().setLevel(1);
            }
        }

        MyScrollView ceView = (MyScrollView) mContext.findViewById(R.id.test_move);
        if (ceView != null) {
            ceView.setCallback(mICallBack2);
            int id = getCEPageId(0);
            if (id != 0) {
                v = mContext.findViewById(id);
                if (v != null) {
                    v.getBackground().setLevel(1);
                }
            }
        }

        registerReceiver();
        updateTime();
        updateRadio();
        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.QUERY_CURRENT_SOURCE, 0);
        BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
    }

    private MyScrollView mMyScrollView;
    private final ICallBack mICallBack = new ICallBack() {
        public void callback(int cmd, int param) {
            if (cmd == MyScrollView.CMD_PAGE_END) {
                Log.d("abcd", "dd:" + param);
                View v;
                try {
                    for (int i = 0; i < 3; ++i) {
                        v = mContext.findViewById(getMidPageId(i));
                        if (v != null) {
                            v.getBackground().setLevel(0);
                        }
                    }
                    v = mContext.findViewById(getMidPageId(param));
                    v.getBackground().setLevel(1);

                } catch (Exception ignored) {
                }
            }
        }
    };

    private int getMidPageId(int i) {
        int id = 0;
        switch (i) {
            case 0:
                id = R.id.mid_page1;
                break;
            case 1:
                id = R.id.mid_page2;
                break;
            case 2:
                id = R.id.mid_page3;
                break;
        }
        return id;
    }

    private final ICallBack mICallBack2 = new ICallBack() {
        public void callback(int cmd, int param) {

            if (cmd == MyScrollView.CMD_PAGE_END) {
                Log.d("abcd", "callback:" + param);
                View v;
                try {
                    for (int i = 0; i < 3; ++i) {
                        int id = getCEPageId(i);
                        if (id != 0) {
                            v = mContext.findViewById(id);
                            if (v != null) {
                                v.getBackground().setLevel(0);
                            }
                        }
                    }
                    int id = getCEPageId(param);
                    if (id != 0) {
                        v = mContext.findViewById(id);
                        if (v != null) {
                            v.getBackground().setLevel(1);
                        }
                    }

                } catch (Exception ignored) {
                }

            }
        }
    };

    private int getCEPageId(int i) {
        int id = 0;
        switch (i) {
            case 0:
                id = R.id.page1_ce;
                break;
            case 1:
                id = R.id.page2_ce;
                break;
            case 2:
                id = R.id.page3_ce;
                break;
        }
        return id;
    }

    public void onClick(View v) {
        Intent it;
        int id = v.getId();
        if (id == R.id.radio_button_play) {
            if (mSource != MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
            }
            BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,MyCmd.Keycode.RADIO_POWER);
        } else if (id == R.id.radio_button_prev) {
            if (mSource == MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,MyCmd.Keycode.PREVIOUS);
            }

            // BroadcastUtil.sendToCarServiceMcuRadio(mContext,
            // ProtocolAk47.SEND_RADIO_SUB_RADIO_OPERATION, 1, 3);
        } else if (id == R.id.radio_button_next) {
            if (mSource == MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI, MyCmd.Keycode.NEXT);
            }

            // BroadcastUtil.sendToCarServiceMcuRadio(mContext,
            // ProtocolAk47.SEND_RADIO_SUB_RADIO_OPERATION, 1, 4);
        } else if (id == R.id.music_button_prev || id == R.id.bt_button_prev) {// setSource(MyCmd.SOURCE_MUSIC);

            if (mSource == MyCmd.SOURCE_MUSIC
                    || mSource == MyCmd.SOURCE_BT_MUSIC) {
                BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,
                        MyCmd.Keycode.PREVIOUS);
            } else if (mSource == MyCmd.SOURCE_DVD) {

                if (MachineConfig.VALUE_SYSTEM_UI21_RM12
                        .equals(Utilities.mSystemUI)) {
                    BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,
                            MyCmd.Keycode.PREVIOUS);
                }
            }
        } else if (id == R.id.music_button_play || id == R.id.bt_button_pp) {
            if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {
                BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI, MyCmd.Keycode.PLAY_PAUSE);
            } else if (mSource == MyCmd.SOURCE_DVD) {
                if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(Utilities.mSystemUI)) {
                    BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI, MyCmd.Keycode.PLAY_PAUSE);
                }
            }
        } else if (id == R.id.music_button_next || id == R.id.bt_button_next) {
            if (mSource == MyCmd.SOURCE_MUSIC
                    || mSource == MyCmd.SOURCE_BT_MUSIC) {

                BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,
                        MyCmd.Keycode.NEXT);
            } else if (mSource == MyCmd.SOURCE_DVD) {

                if (MachineConfig.VALUE_SYSTEM_UI21_RM12
                        .equals(Utilities.mSystemUI)) {
                    BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,
                            MyCmd.Keycode.NEXT);
                }
            }
        } else if (id == R.id.entry_bt_music) {
            UtilCarKey.doKeyBTMusic(mContext);
        } else if (id == R.id.entry_music || id == R.id.entry_music2) {
            if (mSource == MyCmd.SOURCE_BT_MUSIC /*&& (mPlayStatus >= 3)*/) {
                UtilCarKey.doKeyBTMusic(mContext);
            } else {
                if (MachineConfig.VALUE_SYSTEM_UI21_RM12
                        .equals(Utilities.mSystemUI)) {
                    if (mSource == MyCmd.SOURCE_DVD) {
                        UtilCarKey.doKeyDVD(mContext);

                    } else {
                        UtilCarKey.doKeyAudio(mContext);
                    }
                } else {
                    UtilCarKey.doKeyAudio(mContext);
                }
            }
        } else if (id == R.id.img_btn_light) {
            it = new Intent(MyCmd.BROADCAST_START_BACKLIGHTSETTINGS_COMMON);
            it.setPackage("com.my.out");
            mContext.sendBroadcast(it);
        } else if (id == R.id.img_btn_eq) {
            UtilCarKey.doKeyEQ(mContext);
        } else if (id == R.id.img_btn_vol) {
            it = new Intent(MyCmd.BROADCAST_START_VOLUMESETTINGS_COMMON);
            it.setPackage("com.my.out");
            mContext.sendBroadcast(it);

        } else if (id == R.id.img_btn_speed) {
            BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.APP_REQUEST_SEND_KEY, MyCmd.Keycode.SPEED_UP);
        } else if (id == R.id.widget_speed || id == R.id.widget_time || id == R.id.widget_fm) {
            showLeft(v.getId());
        }
    }

    public static void onRadioMusicClick(View v) {
        if (mThis != null) {
            mThis.onClick(v);
        }
    }

    private final static int MSG_UPDATE_TIMECLOCK = 4;
    private final static int MSG_UPDATE_TIMEFLASH = 5;

    Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    updateTime();
                    break;
                case 1:
                    mMusicCurTime += 1000;
                    updateMusicTime();
                    startUpdateMusicTime();
                    break;
                case MSG_UPDATE_TIMECLOCK:
                    updateTimeClock();
                    break;
                case MSG_UPDATE_TIMEFLASH:
                    flashTime(false);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void updateTime() {
        if (!mPause) {
            setTime();
            updateTimeClock();
        }
    }

    private final static int MAX_MUSICTIME = 36000000;

    private void updateMusicTime() {
        String time = "";
        if (mMusicCurTime >= 0 && mMusicCurTime < MAX_MUSICTIME && mMusicTotalTime >= 0 && mMusicTotalTime < MAX_MUSICTIME) {
            if (mMusicTime2 != null) {
                time = stringForTime(mMusicTotalTime);
                mMusicTime2.setText(time);
                time = stringForTime(mMusicCurTime);
            } else {
                if (mMusicTotalTime > 0) {
                    time = stringForTime(mMusicCurTime) + "/" + stringForTime(mMusicTotalTime);
                } else {
                    time = stringForTime(mMusicCurTime);
                }
            }
        }

        mMusicTime.setText(time);

        Log.d("ccb", "updateMusicTime:" + mMusicSeekBar);

        if (mMusicSeekBar != null) {
            int process = 0;
            if (mMusicTotalTime > 0) {
                process = mMusicCurTime * 100 / mMusicTotalTime;
            }
            if (process < 0) {
                process = 0;
            } else if (process > 100) {
                process = 100;
            }
            mMusicSeekBar.setProgress(process);
        }

    }

    private void startUpdateMusicTime() {
        stopUpdateMusicTime();
        mHandler.sendEmptyMessageDelayed(1, 1000);
    }

    private void stopUpdateMusicTime() {
        mHandler.removeMessages(1);
    }

    public byte mMRDBand;
    public byte mMRDNumber = 0;
    public short mMRDFreqency = 8750;

    public short mFreqMax;
    public short mFreqMin;

    private void doMcuData(byte[] buf) {
        int ret = 0;
        if (buf != null && buf.length > 2) {
            switch (buf[1]) {
                case 0x2:
                    if (buf.length > 11) {
                        // Log.d("aa","ffff");
                        mCurPlayIndex = (int) (buf[3] & 0xff);
                        mMRDBand = buf[2];
                        mMRDFreqency = (short) ((((int) buf[4] & 0xff) << 8) | ((int) buf[5]) & 0xff);
                        mFreqMin = (short) ((((int) buf[8] & 0xff) << 8) | ((int) buf[9]) & 0xff);
                        mFreqMax = (short) ((((int) buf[10] & 0xff) << 8) | ((int) buf[11]) & 0xff);
                        updateRadio();
                    }
                    break;
            }
        }
    }

    private void updateRadio() {
        if (mMRDBand <= 2) {
            // mRadioFreq.setText(String.format("%d.%d", mMRDFreqency / 100,
            // mMRDFreqency % 100));
            String s = (mMRDFreqency / 100) + "." + ((mMRDFreqency % 100) / 10)
                    + (mMRDFreqency % 10);
            // if ((mMRDFreqency % 100) == 0 && (mMRDFreqency / 100) != 0) {
            // s += 0;
            // }
            mRadioFreq.setText(s);
            // int index = getCurBaundText();
            mRadioBaud.setText(getCurBaundText());
            mRadioHz.setText("MHz");
            setImageNumRadio(mMRDFreqency, true);
            mMarkFace.setmIsAM(false);

            mMarkFace.setFmFrequencyRange(mFreqMin, mFreqMax);
            MarkFaceView.mFrequencyNum = mMRDFreqency / 100;
        } else {

            mRadioFreq.setText("" + mMRDFreqency);
            setImageNumRadio(mMRDFreqency, false);

            // int index = getCurBaundText();
            mRadioBaud.setText(getCurBaundText());

            mRadioHz.setText("KHz");

            mMarkFace.setmIsAM(true);

            MarkFaceView.mFrequencyNum = (float) ((float) mMRDFreqency);
        }

        mMarkFace.postInvalidate();
    }

    /*
     * private void setTime() { // 24
     *
     * Date curDate = new Date(System.currentTimeMillis()); int h =
     * curDate.getHours();
     *
     * String strTimeFormat = Settings.System.getString(
     * mContext.getContentResolver(),
     * android.provider.Settings.System.TIME_12_24);
     *
     * if ("12".equals(strTimeFormat)) { if (h > 12) { h -= 12; } }
     *
     * mClock.setText(String.format("%02d:%02d", h, curDate.getMinutes())); }
     */

    private void setTime() { // 24

        // Calendar c = Calendar.getInstance();
        // String s;
        //
        // int h = Calendar.HOUR_OF_DAY;
        //
        // String strTimeFormat = Settings.System.getString(
        // mContext.getContentResolver(),
        // android.provider.Settings.System.TIME_12_24);
        //
        // if ("12".equals(strTimeFormat)) {
        // h = c.get(Calendar.HOUR);
        // if (h == 0) {
        // h = 12;
        // }
        // } else {
        //
        // h = c.get(Calendar.HOUR_OF_DAY);
        // }
        //
        // s = String.format("%02d:%02d", h, c.get(Calendar.MINUTE));

        String s = SystemConfig.getProperty(mContext, SystemConfig.KEY_DATE_FORMAT);
        if (s == null) {
            if (MachineConfig.VALUE_SYSTEM_UI16_7099.equals(Utilities.mSystemUI)) {
                s = "MM/dd/yyyy";
            } else {
                //s = "yyyy/MM/dd";
                s = "dd/MM//yyyy";
            }

        }
        long time = System.currentTimeMillis();
        Date d1 = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat(s);
        String t1 = format.format(d1);

        boolean is24 = DateFormat.is24HourFormat(mContext);
        if (is24) {
            format = new SimpleDateFormat("HH:mm");
        } else {
            format = new SimpleDateFormat("hh:mm");
        }
        String t2 = format.format(d1);

        mClock.setText(t2);

        setImageNumTime(t2);
        mDate.setText(t1);
        if (mCEDate != null) {
            mCEDate.setText(t1);
        }

        Calendar c = Calendar.getInstance();

        mDate2.setText(getWeek(c));

        int second = c.get(Calendar.SECOND);
        mHandler.removeMessages(0);

        second = ((60 - second) % 60);
        if (second == 0) {
            second = 60;
        }
        Log.d(TAG, "setTime:"+t2 + " mClock:" + second + " mContext=" + mContext);
        mHandler.sendEmptyMessageDelayed(0, second * 1000);
    }

    private void flashTime(boolean start) {

        if (!mFlashTime || mPause) {
            return;
        }

        if (start) {
            mFlashTimeShow = true;
        } else {
            mFlashTimeShow = !mFlashTimeShow;
        }
        setViewVisible(R.id.digital_colon, mFlashTimeShow ? View.VISIBLE : View.INVISIBLE);
        mHandler.removeMessages(MSG_UPDATE_TIMEFLASH);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMEFLASH, 1000);
    }

    private void setImageNumRadio(int freq, boolean fm) {
        try {
            if (fm) {
                setViewVisible(R.id.num_dot, View.VISIBLE);
                if (freq >= 10000) {
                    setViewVisible(R.id.radionum1, View.VISIBLE);
                    setNumImage(R.id.radionum1, 1);
                } else {
                    setViewVisible(R.id.radionum1, View.GONE);
                }
                setViewVisible(R.id.radionum2, View.VISIBLE);
                setNumImage(R.id.radionum2, (freq / 1000) % 10);
                setNumImage(R.id.radionum3, (freq / 100) % 10);
                setNumImage(R.id.radionum4, (freq / 10) % 10);
                setNumImage(R.id.radionum5, freq % 10);

            } else {
                setViewVisible(R.id.radionum1, View.GONE);
                setViewVisible(R.id.num_dot, View.GONE);
                if (freq >= 1000) {
                    setViewVisible(R.id.radionum2, View.VISIBLE);
                    setNumImage(R.id.radionum2, (freq / 1000) % 10);
                } else {
                    setViewVisible(R.id.radionum2, View.GONE);
                }

                setNumImage(R.id.radionum3, (freq / 100) % 10);
                setNumImage(R.id.radionum4, (freq / 10) % 10);
                setNumImage(R.id.radionum5, freq % 10);
            }
        } catch (Exception ignored) {
        }
    }

    private void setImageNumTime(String time) {
        try {
            String[] ss = time.split(":");
            setNumImage(R.id.timehour_h, Integer.valueOf(ss[0].substring(0, 1)));
            setNumImage(R.id.timehour_l, Integer.valueOf(ss[0].substring(1, 2)));
            setNumImage(R.id.timeminute_h,Integer.valueOf(ss[1].substring(0, 1)));
            setNumImage(R.id.timeminute_l,Integer.valueOf(ss[1].substring(1, 2)));
        } catch (Exception ignored) {
        }
    }

    private void setNumImage(int id, int num) {
        try {
            View v = mContext.findViewById(id);
            if (v == null) {
                return;
            }
            ((ImageView) v).getDrawable().setLevel(num);
        } catch (Exception ignored) {
        }

    }

    public static String getWeek(Calendar cal) {

        int i = cal.get(Calendar.DAY_OF_WEEK);
        return mContext.getString(R.string.week1 + (i - 1));
        /*
         * switch (i) { case 1: return "Sun"; case 2: return "Mon"; case 3:
         * return "Tues"; case 4: return "Wed"; case 5: return "Thur"; case 6:
         * return "Fri"; case 7: return "Sat"; default: return ""+i; }
         */

        /*
         * 　　星期一： Mon.=Monday 　　星期二： Tues.=Tuesday 　　星期三：Wed.=Wednesday 　　星期四：
         * Thur.=Thurday 　　星期五： Fri.=Friday 　　星期六： Sat.=Saturday 　　星期天：
         * Sun.=Sunday
         */
    }

    private BroadcastReceiver mBroadcastReceiver;

    private void unregisterReceiver() {
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    private void setViewVisible(int id, int visibility) {
        View v = mContext.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void updateRadioInfo() {
        Log.d("ffkk", "updateRadioInfo:" + mSource);

        if (mSource == MyCmd.SOURCE_RADIO) {
            mContext.findViewById(R.id.radio_info_layout).setVisibility(
                    View.VISIBLE);
            mContext.findViewById(R.id.radio_button_prev).setVisibility(
                    View.VISIBLE);
            mContext.findViewById(R.id.radio_button_next).setVisibility(
                    View.VISIBLE);

            mMarkFace.setEnable(true);

            mContext.findViewById(R.id.radio_button_prev)
                    .setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View arg0) {
                            // TODO Auto-generated method stub
                            BroadcastUtil.sendKey(mContext,AppConfig.PACKAGE_CAR_UI,MyCmd.Keycode.KEY_SEEK_PREV);
                            return true;
                        }
                    });

            mContext.findViewById(R.id.radio_button_next)
                    .setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View arg0) {
                            // TODO Auto-generated method stub
                            BroadcastUtil.sendKey(mContext,AppConfig.PACKAGE_CAR_UI,MyCmd.Keycode.KEY_SEEK_NEXT);
                            return true;
                        }
                    });

            setViewVisible(R.id.entry_radio, View.VISIBLE);
            setViewVisible(R.id.entry_music, View.GONE);
            setViewVisible(R.id.entry_bt_music, View.GONE);

        } else if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {

            setViewVisible(R.id.entry_radio, View.GONE);
            setViewVisible(R.id.radio_info_layout, View.GONE);
            mMarkFace.setEnable(false);

            if (mBTMusicLayout == null) {
                setViewVisible(R.id.entry_music, View.VISIBLE);
            } else {
                if (mSource == MyCmd.SOURCE_MUSIC) {
                    mBTMusicLayout.setVisibility(View.GONE);
                    setViewVisible(R.id.entry_music, View.VISIBLE);
                } else {
                    mBTMusicLayout.setVisibility(View.VISIBLE);
                    setViewVisible(R.id.entry_music, View.GONE);
                }
            }
        } else {
            // mContext.findViewById(R.id.radio_info_layout).setVisibility(
            // View.GONE);
            //
            // mContext.findViewById(R.id.radio_button_prev).setVisibility(
            // View.INVISIBLE);
            //
            // mContext.findViewById(R.id.radio_button_next).setVisibility(
            // View.INVISIBLE);

            mMarkFace.setEnable(false);

        }

        if (mSource == MyCmd.SOURCE_MUSIC) {
            setViewVisible(R.id.music_icon, View.GONE);
            mMusicName.setVisibility(View.VISIBLE);
            mMusicTime.setVisibility(View.VISIBLE);
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic, null));
            mMusicName.setText(mStrMusicName);
            if (mMusicSeekBar != null)
               mMusicSeekBar.setVisibility(View.VISIBLE);

            if (mMusicPlaying != 1) {
                mMusicTime.setText("");
                if (mMusicSeekBar != null) {
                    mMusicSeekBar.setProgress(0);
                }
            } else {
                setPlayButtonStatus(true);
                startUpdateMusicTime();
            }
        } else if (mSource == MyCmd.SOURCE_BT_MUSIC) {
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.bt_music_pic, null));

            if (mPlayStatus < 3) {
                mMusicName.setText("");
                mMusicTime.setText("");
                setViewVisible(R.id.music_icon, View.VISIBLE);
            } else {
                mMusicName.setText(mID3Name);
                setViewVisible(R.id.music_icon, View.GONE);
            }

            mMusicName.setVisibility(View.VISIBLE);
            mMusicTime.setVisibility(View.VISIBLE);


            mMusicTime.setText("");
            if (mMusicTime2 != null) {
                mMusicTime2.setText("");
            }
            if (mMusicSeekBar != null) {
                mMusicSeekBar.setProgress(0);
                mMusicSeekBar.setVisibility(View.INVISIBLE);
            }
        } else if (mSource == MyCmd.SOURCE_DVD) {
            Log.d(TAG, "updateRadioInfofffffff:" + mSource);
            if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(Utilities.mSystemUI)) {
                setViewVisible(R.id.entry_radio, View.GONE);
                setViewVisible(R.id.radio_info_layout, View.GONE);
                mMusicName.setVisibility(View.VISIBLE);
                mMusicName.setText("");
                mMusicTime.setText("");
                if (mMusicTime2 != null) {
                    mMusicTime2.setText("");
                }
                mMusicTime.setVisibility(View.VISIBLE);
                mMarkFace.setEnable(false);
                mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.dvd_pic));

                mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (mBTMusicLayout == null) {
                    setViewVisible(R.id.entry_music, View.VISIBLE);
                }
            }
        } else {
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic, null));
            setViewVisible(R.id.music_icon, View.VISIBLE);
            mMusicName.setVisibility(View.GONE);
            mMusicTime.setVisibility(View.GONE);
            if (mMusicTime2 != null) {
                mMusicTime2.setText("");
            }
            stopUpdateMusicTime();

            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic, null));
            mMusicName.setText("");
            mMusicTime.setText("");
            if (mMusicSeekBar != null) {
                mMusicSeekBar.setProgress(0);
                mMusicSeekBar.setVisibility(View.VISIBLE);
            }
            setPlayButtonStatus(false);
        }

        if (MachineConfig.VALUE_SYSTEM_UI35_KLD813_2
                .equals(Utilities.mSystemUI)) {
            if (mSource == MyCmd.SOURCE_MUSIC
                    || mSource == MyCmd.SOURCE_BT_MUSIC
                    || mSource == MyCmd.SOURCE_RADIO) {

                setViewVisible(R.id.entry_time, View.GONE);
            } else {
                setViewVisible(R.id.entry_time, View.VISIBLE);
                setViewVisible(R.id.entry_radio, View.GONE);
                setViewVisible(R.id.entry_music, View.GONE);
                setViewVisible(R.id.entry_bt_music, View.GONE);
            }
        }
    }

    private void setPlayButtonStatus(boolean playing) {
        if (playing) {
            ((ImageView) mContext.findViewById(R.id.music_button_play)).getDrawable().setLevel(1);
        } else {
            ((ImageView) mContext.findViewById(R.id.music_button_play)).getDrawable().setLevel(0);
        }
    }

    public static int mSource = MyCmd.SOURCE_NONE;

    public static void setSource(int source) {
        if (mSource != source) {
            BroadcastUtil.sendToCarServiceSetSource(mContext, source);
        }
    }

    @SuppressLint("DefaultLocale")
    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
        // return str;
    }

    public String mStrMusicName = "";

    public int mPlayStatus = A2DP_INFO_INITIAL;
    public String mID3Name = "";
    public String mID3Artist = "";
    public String mID3Album = "";

    public static final int A2DP_INFO_INITIAL = 0;
    public static final int A2DP_INFO_READY = 1;
    public static final int A2DP_INFO_CONNECTING = 2;
    public static final int A2DP_INFO_CONNECTED = 3;
    public static final int A2DP_INFO_PLAY = 4;
    public static final int A2DP_INFO_PAUSED = 5;

    private void doBTCmd(Intent intent) {
        int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);

        switch (cmd) {
            case MyCmd.Cmd.BT_SEND_A2DP_STATUS: {
                int play = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);

                // String name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA2);
                // String pin = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA3);

                // if (name != null) {
                // mBtName = name;
                // }
                // if (pin != null) {
                // mBtPin = pin;
                // }

                if (play != mPlayStatus) {
                    mPlayStatus = play;
                    // updateConnectView();
                }
                if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                    setPlayButtonStatus(mPlayStatus == A2DP_INFO_PLAY);
                    if (mPlayStatus < 3) {
                        mMusicName.setText("");
                        mMusicTime.setText("");
                        if (mMusicTime2 != null) {
                            mMusicTime2.setText("");
                        }
                        if (mMusicSeekBar != null) {
                            mMusicSeekBar.setProgress(0);
                        }
                        setViewVisible(R.id.music_icon, View.VISIBLE);
                        stopUpdateMusicTime();
                    } else {
                        //startUpdateMusicTime();
                        setViewVisible(R.id.music_icon, View.GONE);
                    }
                }

            }
            break;
            case MyCmd.Cmd.BT_SEND_ID3_INFO: {
                mID3Name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA);
                if (mSource == MyCmd.SOURCE_BT_MUSIC && mPlayStatus >= A2DP_INFO_CONNECTED) {
                    mMusicName.setText(mID3Name);
                }

                // Log.d("allen", mID3Name);
                // mID3Artist = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA2);
                // mID3Album = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA3);
                // updateView();
            }
            break;
            case MyCmd.Cmd.BT_SEND_TIME_STATUS: {
                int t = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0) * 1000;
                mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, 0) * 1000;


                if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                    if ((t - mMusicCurTime) > 1000 || (t - mMusicCurTime) < -1000) {
                        mMusicCurTime = t;
                        //updateMusicTime();
                    }
                } else {
                    mMusicCurTime = t;
                }
                // updateView();
            }
            default:
                return;
        }
    }

    private Bitmap mArtWork;

    private Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    private void registerReceiver() {
        if (mBroadcastReceiver == null)
        {
            mBroadcastReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
                    Log.d(TAG, "BROADCAST_CMD_FROM_MUSIC Action:" + action+",CMD:"+cmd);
                    switch (Objects.requireNonNull(action))
                    {
                        case Intent.ACTION_TIME_CHANGED:
                            updateTime();
                            break;
                        case MyCmd.BROADCAST_CMD_FROM_MUSIC:
                            Log.d(TAG, "BROADCAST_CMD_FROM_MUSIC Source:" + mSource);
                            switch (mSource)
                            {
                                case MyCmd.SOURCE_MUSIC:
                                    //int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
                                    if (cmd == MyCmd.Cmd.MUSIC_SEND_PLAY_STATUS)
                                    {
                                        int data = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
                                        mMusicCurTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, -1);
                                        mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA3, -1);

                                        //Object obj = intent.getExtra(MyCmd.EXTRA_COMMON_OBJECT);mackylee
                                        byte[] buff = intent.getByteArrayExtra(MyCmd.EXTRA_COMMON_OBJECT);

                                        if (mSource != MyCmd.SOURCE_BT_MUSIC) {
                                            if (buff != null) {
                                                mArtWork = Bytes2Bimap(buff);//(Bitmap) obj;
                                                mArtWork = createReflectedImage(mArtWork, 80);
                                                mMusicArt.setImageBitmap(mArtWork);
                                                mMusicArt.getDrawable().setDither(true);
                                                mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);

                                                // mMusicArt.setImageBitmap(mArtWork);
                                            } else {
                                                mArtWork = null;
                                                mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic, null));
                                                mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                            }
                                        }
                                        String name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA4);
                                        mStrMusicName = name;
                                        mMusicPlaying = data;
                                        if (mSource == MyCmd.SOURCE_MUSIC) {
                                            // Log.d("allen", "music name:"+name);
                                            mMusicName.setText(name);
                                            setPlayButtonStatus(data == 1);
                                            updateMusicTime();
                                        }

                                        if (data == 1) {
                                            if (mSource == MyCmd.SOURCE_MUSIC) {
                                                startUpdateMusicTime();
                                            }
                                        } else {
                                            stopUpdateMusicTime();
                                        }

                                    }
                                    break;
                                case MyCmd.SOURCE_DVD:
                                    if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(Utilities.mSystemUI))
                                    {
                                        int data = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
                                        int time = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, -1);
                                        int song = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA3, -1);
                                        int tag = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA4, -1);

                                        if (tag == 0xabc)
                                        {
                                            if (song == -1) {
                                                mMusicName.setText("DVD");
                                            } else {
                                                if (song != 0) {
                                                    mMusicName.setText("CD Track " + song);
                                                }
                                            }
                                            mMusicName.setVisibility(View.VISIBLE);
                                            mMusicTime.setVisibility(View.VISIBLE);
                                            time *= 1000;
                                            String s = stringForTime(time);
                                            mMusicTime.setText(s);

                                            setPlayButtonStatus(data == 1);
                                        }
                                        stopUpdateMusicTime();
                                        // if(data == 1){
                                        //
                                        // startUpdateMusicTime();
                                        //
                                        // } else {
                                        // stopUpdateMusicTime();
                                        // }
                                    }
                                    break;
                            }
                            break;
                        case MyCmd.BROADCAST_CAR_SERVICE_SEND:
                            ///int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
                            /// Log.d("tt", "cmd:" + cmd);
                            switch (cmd) {
                                case MyCmd.Cmd.MCU_RADIO_RECEIVE_DATA:
                                    byte[] buf = intent.getByteArrayExtra(MyCmd.EXTRA_COMMON_DATA);
                                    doMcuData(buf);
                                    break;

                                case MyCmd.Cmd.SOURCE_CHANGE:
                                case MyCmd.Cmd.RETURN_CURRENT_SOURCE:
                                    mSource = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, MyCmd.SOURCE_NONE);
                                    if (mSource != MyCmd.SOURCE_MUSIC) {
                                        stopUpdateMusicTime();
                                    }

                                    if (cmd == MyCmd.Cmd.RETURN_CURRENT_SOURCE) {
                                        switch (mSource) {
                                            case MyCmd.SOURCE_RADIO:
                                                BroadcastUtil.sendToCarServiceMcuRadio(mContext,ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO,0);
                                                break;
                                            case MyCmd.SOURCE_MUSIC:
                                                Intent i = new Intent(MyCmd.BROADCAST_CMD_TO_MUSIC);
                                                i.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.MUSIC_REQUEST_PLAY_STATUS);
                                                mContext.sendBroadcast(i);
                                                break;
                                            case MyCmd.SOURCE_BT_MUSIC:
                                                Intent it = new Intent(MyCmd.BROADCAST_CMD_LAUNCHER_TO_BT);
                                                it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_REQUEST_A2DP_INFO);
                                                mContext.sendBroadcast(it);
                                                break;
                                            default:
                                                    ///if (mSource == MyCmd.SOURCE_DVD) {
                                                    // if
                                                    // (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(Utilities.mSystemUI)
                                                    // ){
                                                    // Intent i = new
                                                    // Intent(MyCmd.BROADCAST_CMD_TO_MUSIC);
                                                    // i.putExtra(MyCmd.EXTRA_COMMON_CMD,
                                                    // MyCmd.Cmd.MUSIC_REQUEST_PLAY_STATUS);
                                                    //
                                                    // mContext.sendBroadcast(i);
                                                    // }
                                                //}
                                                break;
                                        }
                                    }

                                    updateRadioInfo();
                                    break;
                                case MyCmd.Cmd.LAUNCHER_SHOW_ALL_APP:
                                    //mContext.onClickAllAppsButton(null);
                                    break;
                                case MyCmd.Cmd.REVERSE_STATUS:
                                    if (mSource != MyCmd.SOURCE_MUSIC)
                                        BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI, MyCmd.Keycode.PAUSE);

                                    if (mSource != MyCmd.SOURCE_RADIO) {
                                        BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
                                        BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI,MyCmd.Keycode.RADIO_POWER);
                                    }
                                    break;
                            }

                            break;
                        case MyCmd.BROADCAST_CMD_FROM_BT:
                            doBTCmd(intent);
                            break;
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            intentFilter.addAction(MyCmd.BROADCAST_CAR_SERVICE_SEND);
            intentFilter.addAction(MyCmd.BROADCAST_CMD_FROM_MUSIC);
            intentFilter.addAction(MyCmd.BROADCAST_CMD_FROM_BT);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private int mMusicPlaying = 0;

    private Bitmap createReflectedImage(Bitmap paramBitmap, int paramInt) {
        Bitmap localBitmap2 = null;
        try {
            int i = paramBitmap.getWidth();
            int j = paramBitmap.getHeight();
            Matrix localMatrix = new Matrix();
            localMatrix.preScale(1.0F, -1.0F);
            Bitmap localBitmap1 = Bitmap.createBitmap(paramBitmap, 0, (j - paramInt) > 0 ? (j - paramInt) : 0, i, paramInt, localMatrix, false);
            localBitmap2 = Bitmap.createBitmap(i, j + paramInt,  Bitmap.Config.ARGB_8888);
            Canvas localCanvas = new Canvas(localBitmap2);
            Paint localPaint1 = new Paint();
            localCanvas.drawBitmap(paramBitmap, 0.0F, 0.0F, localPaint1);
            localCanvas.drawBitmap(localBitmap1, 0.0F, j, localPaint1);
            Paint localPaint2 = new Paint();
            localPaint2.setShader(new LinearGradient(0.0F, paramBitmap.getHeight(), 0.0F, localBitmap2.getHeight(), 1895825407,
                    16777215, Shader.TileMode.MIRROR));
            localPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            localCanvas.drawRect(0.0F, j, i, localBitmap2.getHeight(), localPaint2);
            localBitmap1.recycle();
        } catch (Exception ignored) {
        }

        if (localBitmap2 == null) {
            return paramBitmap;
        }
        return localBitmap2;
    }

    public int mCurPlayIndex = 0;

    public static final int MRD_FM1 = 0;
    public static final int MRD_FM2 = 1;
    public static final int MRD_FM3 = 2;
    public static final int MRD_AM = 3;
    public static final int MRD_AM2 = 3;

    int mFMNum = 1;

    public String getCurBaundText() {
        String s;
        int index = 1;

        if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(Utilities.mSystemUI)) {
            index = mCurPlayIndex / 6;
            if (mMRDBand >= MRD_AM) {
                s = "AM";

                if (index < 0 || index > 2) {
                    index = 0;
                }
            } else {
                s = "FM";
                if (index < 0 || index > 5) {
                    index = 0;
                }
            }

            s += (index + 1);
        } else {
            if (mMRDBand >= MRD_AM) {
                s = "AM";
                index = mMRDBand - MRD_AM;
            } else {
                s = "FM";
                index = mMRDBand - MRD_FM1;
            }

            if (mFMNum == 3) {
                s += (index + 1);
            }
        }
        return s;
    }

    // ///////for clock
    private View mViewHour;
    private View mViewMinutus;
    private View mViewSec;

    private void updateTimeClock() {

        if (mViewHour != null) {

            Date curDate = new Date(System.currentTimeMillis());
            int h = curDate.getHours();

            String strTimeFormat = Settings.System.getString(
                    mContext.getContentResolver(),
                    android.provider.Settings.System.TIME_12_24);

            if ("12".equals(strTimeFormat)) {
                if (h > 12) {
                    h -= 12;
                }
            }

            int m = curDate.getMinutes();
            int s = curDate.getSeconds();

            float rotate;

            rotate = ((h * 60 + m) / 720.0f) * 360.0f;// (h * 360.0f) / 12.0f;

            mViewHour.setRotation(rotate);

            rotate = (m * 360.0f) / 60.0f;

            mViewMinutus.setRotation(rotate);

            rotate = (s * 360.0f) / 60.0f;

            mViewSec.setRotation(rotate);

            mHandler.removeMessages(MSG_UPDATE_TIMECLOCK);
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMECLOCK, 1000);

        }

    }

    private View mSpeedPoint;
    private TextView mTvSpeed;
    private TextView mTvAtitude;
    private float mSpeed = 0;

    private View mBearingPoint;
    private final static float SPEED_ANGLE_0 = 250.5f;// 0 start //913
    private final static float SPEED_ANGLE_0_KLD007 = 220.0f;// 0 start //kld007

    private final static float SPEED_MID = 120.0f;// 0 start
    private final static float SPEED_MAX = 240.0f;// 0 start

    private void updateSpeed() {
//		Log.d(TAG2, "updateSpeed:" + mSpeed);
        if (MachineConfig.VALUE_SYSTEM_UI42_913.equals(Utilities.mSystemUI)) {
            if (mLeftId != R.id.widget_speed) {
                return;
            }
        }
        if (mSpeedPoint != null) {
            float rotate = SPEED_ANGLE_0;
            if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
                rotate = SPEED_ANGLE_0_KLD007;
            }
            float step;
            float angle;
            if (mSpeed < SPEED_MID) {
                angle = 360.0f - SPEED_ANGLE_0;
                step = mSpeed / SPEED_MID;
                rotate += (angle * step);
            } else {
                angle = 360.0f - SPEED_ANGLE_0;
                step = (mSpeed - SPEED_MID) / (SPEED_MAX - SPEED_MID);
                rotate = 360.0f + (angle * step);
            }

            mSpeedPoint.setRotation(rotate);

        }

        if (mTvSpeed != null) {
            mTvSpeed.setText(((int) mSpeed) + "");
        }

    }


    private LocationListener mLocationListener = new LocationListener() {

        /**
         * 位置信息变化时触发
         */
        public void onLocationChanged(Location location) {
            // location.getAltitude(); -- 海拔
            mSpeed = (location.getSpeed() * 3.6f);

            Log.d(TAG2, "onLocationChanged:" + location.toString());
            updateSpeed();

            if (mTvAtitude != null) {
                int mAltitude = (int) (location.getAltitude() * 100);
                String s = mAltitude / 100 + "." + mAltitude % 100 + "m";
                mTvAtitude.setText(s);
            }
            if (mBearingPoint != null) {
                mBearingPoint.setRotation(location.getBearing());
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {

        }

        public void onProviderDisabled(String provider) {
        }

    };

    private int mLeftId = R.id.widget_time;
    private LocationManager mLocationManager;

    private void initGPSInfo() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            Log.d(TAG, "init gps:" + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (mLocationListener != null) {
                    mLocationManager.removeUpdates((LocationListener) mLocationListener);
                }
                if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            }
        }
    }

    private void initMoreInfo() {
        if (MachineConfig.VALUE_SYSTEM_UI42_913.equals(Utilities.mSystemUI)) {
            showLeft(mLeftId);

            initGPSInfo();
        }


        if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
            initGPSInfo();

            updateSpeed();
        }
    }

    private void showLeft(int id) {

        mLeftId = id;
        mContext.findViewById(R.id.layout_left_time).setVisibility(View.GONE);
        mContext.findViewById(R.id.layout_left_speed).setVisibility(View.GONE);
        mContext.findViewById(R.id.layout_left_fm).setVisibility(View.GONE);
        mContext.findViewById(R.id.widget_tag1).setVisibility(View.INVISIBLE);
        mContext.findViewById(R.id.widget_tag2).setVisibility(View.INVISIBLE);
        mContext.findViewById(R.id.widget_tag3).setVisibility(View.INVISIBLE);

        if (id == R.id.widget_speed) {
            mContext.findViewById(R.id.layout_left_speed).setVisibility(View.VISIBLE);
            mContext.findViewById(R.id.widget_tag1).setVisibility(View.VISIBLE);
            updateSpeed();
        } else if (id == R.id.widget_time) {
            mContext.findViewById(R.id.layout_left_time).setVisibility(View.VISIBLE);
            mContext.findViewById(R.id.widget_tag2).setVisibility(View.VISIBLE);
            updateTimeClock();
        } else if (id == R.id.widget_fm) {
            mContext.findViewById(R.id.layout_left_fm).setVisibility(View.VISIBLE);
            mContext.findViewById(R.id.widget_tag3).setVisibility(View.VISIBLE);
        }

    }

    //dark mode
    private static void setBackground(int id, int resid) {
        if (mContext != null) {
            View v = mContext.findViewById(id);
            if (v != null) {
                v.setBackgroundResource(resid);
            }
        }
    }

    private static void setImageSrc(int id, int resid) {
        if (mContext != null) {
            View v = mContext.findViewById(id);
            if (v != null) {
                ((ImageView) v).setImageResource(resid);
            }
        }
    }

    private static final int[][] DARK_UI = {
            {R.id.entry_time, R.drawable.time_back, R.drawable.time_back_d},
            {R.id.entry_time1, R.drawable.time_back, R.drawable.time_back_d},
            {R.id.layout_right1, R.drawable.music_back, R.drawable.time_back_d},
            {R.id.layout_right2, R.drawable.music_back, R.drawable.time_back_d},

            {R.id.layout_mid1, R.drawable.left_back, R.drawable.left_back_d},
            {R.id.layout_mid2, R.drawable.left_back, R.drawable.left_back_d},
            {R.id.layout_mid3, R.drawable.left_back, R.drawable.left_back_d},
            {R.id.b_line, R.drawable.b_line, R.drawable.b_line_d},


            {R.id.img_btn_vol, R.drawable.app_volume, R.drawable.app_volume_d},
            {R.id.button_gps, R.drawable.app_navi, R.drawable.app_navi_d},
            {R.id.button_music, R.drawable.app_music, R.drawable.app_music_d},
            {R.id.button_video, R.drawable.app_video, R.drawable.app_video_d},
            {R.id.all_apps_button, R.drawable.app_apps, R.drawable.app_apps_d},
            {R.id.button_bluetooth, R.drawable.app_bt, R.drawable.app_bt_d},
            {R.id.button_radio, R.drawable.app_radio, R.drawable.app_radio_d},
            {R.id.button_settings, R.drawable.app_set, R.drawable.app_set_d},
            {R.id.img_btn_light, R.drawable.app_backlight, R.drawable.app_backlight_d},
    };

    public static void doDarkMode(boolean dark) {

        int res;
        int drawable;
        for (int i = 0; i < DARK_UI.length; ++i) {
            res = DARK_UI[i][0];

            if (dark) {
                drawable = DARK_UI[i][2];

            } else {
                drawable = DARK_UI[i][1];

            }

            if (i > 7) {
                setImageSrc(res, drawable);
            } else {
                setBackground(res, drawable);
            }

        }

    }
}
