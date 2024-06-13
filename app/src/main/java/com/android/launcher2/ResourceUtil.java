package com.android.launcher2;

import com.common.util.MachineConfig;
import com.common.util.SystemConfig;
import com.common.util.AppConfig;
import com.android.launcher.R;
import com.zhuchao.android.fbase.MMLog;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;

import java.util.Arrays;

public class ResourceUtil {
    private static final String TAG = "ResourceUtil";
    public static int getLayoutId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "layout",paramContext.getPackageName());
    }

    public static int getStringId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "string",paramContext.getPackageName());
    }

    public static int getDrawableId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString,"drawable", paramContext.getPackageName());
    }

    public static int getStyleId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "style", paramContext.getPackageName());
    }

    public static int getId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "id", paramContext.getPackageName());
    }

    public static int getColorId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "color",paramContext.getPackageName());
    }

    public static void setActivityAnim(Activity c, int anim) {
        View v = ((ViewGroup) c.findViewById(android.R.id.content)).getChildAt(0);// getWindow().getDecorView();

        Animation animation = (AnimationSet) AnimationUtils.loadAnimation(c,anim);

        v.clearAnimation();
        v.setAnimation(animation);

        animation.start();
    }

    // it must call before setContentView
    public static int sw = 0;
    private final static String LAUNCHER_UI = "launcher_ui";
    public static int mScreenWidth;
    public static int mScreenHeight;

    public static String updateUi(Context context) { // only launcher use now

        String value = MachineConfig.getPropertyReadOnly(LAUNCHER_UI);
        MMLog.d(TAG, "MachineConfig.getPropertyReadOnly(LAUNCHER_UI)=" +value);
        if (value == null) {
            value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);
            MMLog.d(TAG, "MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI)=" +value);
        }

        if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(value) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(value))
        {
            String s = SystemConfig.getProperty(context, SystemConfig.KEY_LAUNCHER_UI_RM10);
            if (s != null) {
                if ("1".equals(s)) {
                    value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
                } else { //0
                    value = MachineConfig.VALUE_SYSTEM_UI20_RM10_1;
                }
            }
        }

        else if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(value)) {
            String s = SystemConfig.getProperty(context, SystemConfig.KEY_LAUNCHER_UI_RM10);
            if (s != null) {
                if ("1".equals(s)) {
                    value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
                    Utilities.mIconPath = "/mnt/paramter/icon/rm10_2/";
                }
            }
        }

        int dsp = SystemConfig.getIntProperty(context, SystemConfig.KEY_DSP);
        Utilities.mIsDSP = (dsp == 1);
        Utilities.mSystemUI = value;
        MMLog.d(TAG, "Utilities.mIsDSP=" + Utilities.mIsDSP);

		//int sw = 0;
        ///int w = 0;
        ///int h = 0;
        ///int type = 0; // deault 800X480

        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays();
        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        MMLog.d(TAG,"Display[]=" + Arrays.toString(display));
        MMLog.d(TAG,"DisplayMetrics=" + dm.toString());
        MMLog.d(TAG,"Configuration1:"+configuration.toString());

        if (dm.widthPixels == 1024 && dm.heightPixels == 600) {//667
            sw = 321;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 480) {
            sw = 320;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 720) {//667
            sw = 321;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 800) {
            sw = 321;
        } else {
            sw = 320;
        }

        configuration.smallestScreenWidthDp = sw;
        context.getResources().updateConfiguration(configuration, null);
        MMLog.d(TAG,"Configuration2:" + configuration.toString());
        return value;
    }

    public static Bitmap toRoundBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
        if (width <= height) {
            roundPx = width / 2;
            top = 0;
            bottom = width;
            left = 0;
            right = width;
            height = width;
            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;
            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }
        Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src = new Rect((int) left, (int) top, (int) right,
                (int) bottom);
        final Rect dst = new Rect((int) dst_left, (int) dst_top,
                (int) dst_right, (int) dst_bottom);
        final RectF rectF = new RectF(dst);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        return output;
    }

    public static boolean ifLoadDvdHideWorkspace() {
        if (Utilities.mSystemUI != null
                && (Utilities.mSystemUI
                .equals(MachineConfig.VALUE_SYSTEM_UI21_RM10_2)
                || Utilities.mSystemUI
                .equals(MachineConfig.VALUE_SYSTEM_UI22_1050)
                || Utilities.mSystemUI
                .equals(MachineConfig.VALUE_SYSTEM_UI45_8702_2)
                || Utilities.mSystemUI
                .equals(MachineConfig.VALUE_SYSTEM_UI35_KLD813_2)))
        {
            if (AppConfig.isHidePackage("com.my.dvd.DVDPlayer") && !AppConfig.isUSBDvd())
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
            return false;
    }
}
