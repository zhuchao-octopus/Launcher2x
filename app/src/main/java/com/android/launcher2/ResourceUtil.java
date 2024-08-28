package com.android.launcher2;

import com.common.utils.MachineConfig;
import com.common.utils.SettingProperties;
import com.common.utils.AppConfig;
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
            String s = SettingProperties.getProperty(context, SettingProperties.KEY_LAUNCHER_UI_RM10);
            if (s != null) {
                if ("1".equals(s)) {
                    value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
                } else { //0
                    value = MachineConfig.VALUE_SYSTEM_UI20_RM10_1;
                }
            }
        }

        else if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(value)) {
            String s = SettingProperties.getProperty(context, SettingProperties.KEY_LAUNCHER_UI_RM10);
            if (s != null) {
                if ("1".equals(s)) {
                    value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
                    Utilities.mIconPath = "/mnt/paramter/icon/rm10_2/";
                }
            }
        }

        int dsp = SettingProperties.getIntProperty(context, SettingProperties.KEY_DSP);
        Utilities.mIsDSP = (dsp == 1);
        Utilities.mSystemUI = value;
        MMLog.d(TAG, "Utilities.mIsDSP=" + Utilities.mIsDSP);


		//int sw = 0;
        ///int w = 0;
        ///int h = 0;
        int type = 0; // deault 800X480

        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays();
        Configuration configuration = context.getResources().getConfiguration();
        MMLog.d(TAG, "Display[]=" + Arrays.toString(display));
		//Rect outRect = new Rect();
		//display[0].getOverscanInsets(outRect);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        MMLog.d(TAG,"DisplayMetrics=" + dm.toString());
        MMLog.d(TAG,"configuration:"+configuration.toString());


        if (dm.widthPixels == 1024 && dm.heightPixels == 600) {//667
            type = 1;
            sw = 321;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 480) {
            type = 2;
            sw = 320;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 720) {//667
            type = 3;
            sw = 321;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 800) {
            type = 3;
            sw = 321;
        } else {
            type = 0;
            sw = 320;
        }

        if (MachineConfig.VALUE_SYSTEM_UI_KLD1.equals(value)) {
            if (type == 0) {
                sw = 322;
                // w = 801;
                // h = 479;
            } else {
                sw = 323;
                // w = 1025;
                // h = 601;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD2.equals(value) ||
                MachineConfig.VALUE_SYSTEM_UI_KLD15_6413.equals(value) ||
                MachineConfig.VALUE_SYSTEM_UI24_616.equals(value)) {
            if (type == 0) {
                sw = 324;
            } else if (type == 2) {
                sw = 324;
            } else {
                sw = 325;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD3.equals(value)) {
            if (type == 0) {
                sw = 326;
            } else {
                sw = 327;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD5.equals(value)) {
            if (type == 0) {
                sw = 328;
            } else {
                sw = 329;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD7_1992.equals(value)) {
            if (type == 0) {
                sw = 330;
            } else {
                sw = 331;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD3_8702.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(value)) {
            if (type == 0) {
                sw = 332;
            } else {
                sw = 333;
            }

			///if (MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(value)) {
			///	h = 441;
			///}
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD10_887.equals(value)) {
            if (type == 0) {
                sw = 334;
            } else if (type == 2) {
                sw = 335;
            } else {
                sw = 336;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(value)) {
            if (type == 0) {
                sw = 338;
            } else if (type == 2) {
                sw = 340;
            } else {
                sw = 339;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(value)) {
            if (type == 0) {
                sw = 344;
            } else if (type == 2) {
                sw = 346;
            } else {
                sw = 345;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI16_7099.equals(value)) {
            if (type == 0) {
                sw = 348;
            } else {
                sw = 349;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(value)) {
            if (type == 0) {
                sw = 352;
            } else {
                sw = 353;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(value)) {
            if (type == 0) {
                sw = 356;
            } else {
                sw = 357;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI35_KLD813_2.equals(value)) {
            if (type == 0) {
                sw = 360;    //800x480
            } else {
                sw = 361;    //1024x600
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(value)) {
            if (type == 0) {
                sw = 362;    //800x480
            } else {
                sw = 363;    //1024x600
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI40_KLD90.equals(value) ||
                MachineConfig.VALUE_SYSTEM_UI_9813.equals(value)) {
            if (type == 0) {
                sw = 364; // 800x480
            } else if (type == 2) {
                sw = 366; // 1280x480
            } else if (type == 3) {
                sw = 367; // 1280x720
            } else {
                sw = 365; // 1024x600
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI41_2007.equals(value)) {
            if (type == 0) {
                sw = 372;    //800x480
            } else {
                sw = 373;    //1024x600
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI42_913.equals(value)
                || MachineConfig.VALUE_SYSTEM_UI42_13.equals(value)) {

            if (type == 0 || type == 2) {
                sw = 368;    //800x480
            } else {
                sw = 369;    //1024x600
            }

            ///if (MachineConfig.VALUE_SYSTEM_UI42_13.equals(value)) {
            ///   h = 450;
            ///}

        } else if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(value)) {
            if (type == 0) {
                sw = 380;    //800x480
            } else {
                sw = 381;    //1024x600
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_887_90.equals(value)) {
            if (type == 0) {
                sw = 386;
            } else if (type == 2) {
                sw = 387;
            } else {
                sw = 388;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(value)) {
            if (type == 0) {
                sw = 400;
            } else if (type == 2) {
                sw = 401;
            } else {
                sw = 402;
            }
        }


        configuration.smallestScreenWidthDp = sw;
        ///if (w != 0) {
        ///    c.screenWidthDp = w;
        ///}
        ///if (h != 0) {
        ///    c.screenHeightDp = h;
        ///}

        ///MMLog.d(TAG, "w=" + w + ",h=" + h);
        context.getResources().updateConfiguration(configuration, null);
        MMLog.d(TAG, "sw:" + sw + ",configuration:" + configuration.toString());
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
