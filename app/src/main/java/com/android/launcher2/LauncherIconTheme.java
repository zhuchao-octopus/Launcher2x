package com.android.launcher2;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.android.launcher.R;

public final class LauncherIconTheme {
    private final static String TAG = "LauncherIconTheme";

    //根据包名、类名获取Bitmap
    public static Bitmap getIconBitmap(Context context, String packageName, String className) {
        Resources resources = context.getResources();
        int iconId = getIconId(packageName, className);
        if (iconId != -1) {
            return BitmapFactory.decodeResource(resources, iconId);
        }
        return null;
    }

    public static Bitmap getAppIconBitmap(Context context, String packageName, String className)
    {
        Drawable d = getAppIconDrawable(context,packageName,className);
        if(d != null)
             return Utilities.createIconBitmap(d, context);
        else
             return null;
    }

    public static Drawable getAppIconDrawable(Context context, String packageName, String className) {
        Resources resources = context.getResources();
        int iconId = getAppIconId(packageName, className);
        if (iconId != -1) {
            return resources.getDrawable(iconId,null);
        }
        return null;
    }

    private static int getAppIconId(String packageName, String className) {
        if(packageName.equals("com.zoulou.dab") && className.equals("com.zoulou.dab.activity.MainActivity"))
        {
            //return R.drawable.ic_dab;
        }

        if(packageName.equals("com.my.bt") && className.equals("com.my.bt.ATBluetoothActivity"))
        {
            //return R.drawable._blutooth_call;
        }

        if(packageName.equals("com.suding.speedplay") && className.equals("com.suding.speedplay.ui.MainActivity"))
        {
            return R.drawable.app_carplay;
        }

        if(packageName.equals("com.car.ui") && className.equals("com.my.radio.RadioActivity"))
        {
            return R.drawable.app_radio;
        }
        if(packageName.equals("com.car.ui") && className.equals("com.my.video.VideoActivity"))
        {
            return R.drawable.app_video;
        }

        if(packageName.equals("com.car.ui") && className.equals("com.my.audio.MusicActivity"))
        {
            return R.drawable.app_music;
        }

        if(packageName.equals("com.car.ui") && className.equals("com.my.btmusic.BTMusicActivity"))
        {
            return R.drawable.app_blutooth_music;
        }

        if(packageName.equals("com.car.ui") && className.equals("com.my.auxplayer.AUXPlayer"))
        {
            return R.drawable.app_auxin;
        }

        if(packageName.equals("com.car.ui") && className.equals("com.my.frontcamera.FrontCameraActivity"))
        {
            return R.drawable.app_f_camara;
        }

        if(packageName.equals("com.android.settings") && className.equals("com.android.settings.homepage.SettingsHomepageActivity"))
        {
            return R.drawable.app_setting;
        }

        if(packageName.equals("com.anydesk.anydeskandroid") && className.equals("com.anydesk.anydeskandroid.gui.activity.MainActivity"))
        {
            //return R.drawable.apps_anydesk;
        }
        if(packageName.equals("com.my.instructions") && className.equals("com.my.instructions.InstructionsActivity"))
        {
            return R.drawable.app_manual;
        }
        if(packageName.equals("com.android.gallery3d") && className.equals("com.android.gallery3d.app.GalleryActivity"))
        {
            return R.drawable.app_pictures;
        }
        if(packageName.equals("com.SwcApplication") && className.equals("com.SwcApplication.SwcActivity"))
        {
            return R.drawable.app_fk;
        }

        if(packageName.equals("com.android.deskclock") && className.equals("com.android.deskclock.DeskClock"))
        {
            return R.drawable.app_clock;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.CanAirControlActivity"))
        {
            //return R.drawable.car_acc;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.MainActivity"))
        {
            return R.drawable.car_3;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.CarInfoActivity"))
        {
            return R.drawable.car_1;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.JeepCarCDPlayerActivity"))
        {
            return R.drawable.car_2;
        }

        if(packageName.equals("com.google.android.apps.maps") && className.equals("com.google.android.maps.MapsActivity"))
        {
            return R.drawable.app_googlemap;
        }

        if(packageName.equals("com.google.android.youtube"))
        {
            return R.drawable.app_youtube;
        }

        if(packageName.equals("com.my.fileexplorer")&& className.equals("com.my.fileexplorer.FileManagerActivity"))
        {
            //return R.drawable.app_1;
        }
        if(packageName.equals("com.android.calculator2")&& className.equals("com.android.calculator2.Calculator"))
        {
            return R.drawable.app_caculator;
        }
        if(packageName.equals("com.android.settings") && className.equals("com.android.settings.Settings"))
        {
            return R.drawable.app_setting;
        }

        if(packageName.equals("com.google.android.googlequicksearchbox") && className.equals("com.google.android.googlequicksearchbox.VoiceSearchActivity"))
        {
            //return R.drawable.app_seach;
        }

        if(packageName.equals("com.android.documentsui") && className.equals("com.android.documentsui.LauncherActivity"))
        {
            return R.drawable.app_foder;
        }

        if(packageName.equals("com.my.filemanager") && className.equals("com.my.filemanager.FileManagerActivity"))
        {
            return R.drawable.app_apk;
        }

        if(packageName.equals("com.android.chrome") && className.equals("com.google.android.apps.chrome.Main"))
        {
            return R.drawable.app_chrome;
        }

        if(packageName.equals("com.eqset") && className.equals("com.eqset.EQActivity"))
        {
            return R.drawable.app_dsp;
        }

        if(packageName.equals("com.car.dvdplayer") && className.equals("com.car.dvdplayer.DVDPlayerActivity"))
        {
            //return R.drawable.app_2;
        }

        if(packageName.equals("com.my.dvr") && className.equals("com.my.dvr.MainActivity"))
        {
            return R.drawable.app_dvr;
        }

        if(packageName.equals("com.google.android.googlequicksearchbox") && className.equals("com.google.android.googlequicksearchbox.SearchActivity"))
        {
            return R.drawable.app_google;
        }

        if(packageName.equals("com.android.calendar") && className.equals("com.android.calendar.AllInOneActivity"))
        {
            return R.drawable.app_calendar;
        }

        if(packageName.equals("com.android.vending") && className.equals("com.android.vending.AssetBrowserActivity"))
        {
            return R.drawable.app_playstore;
        }

        if(packageName.equals("com.android.quicksearchbox") && className.equals("com.android.quicksearchbox.SearchActivity"))
        {
            return R.drawable.app_seach;
        }
        if(packageName.equals("com.car.ui") && className.equals("com.my.navi.NaviEmptyActivity"))
        {
            return R.drawable.ic_navigate;
        }
        return -1;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //根据包名、类名获取Drawable   要用到的就是这个方法
    public static Drawable getIconDrawable(Context context, String packageName, String className) {
        Resources resources = context.getResources();
        int iconId = getIconId(packageName, className);
        if (iconId != -1) {
            return resources.getDrawable(iconId,null);
        }
        return null;
    }

    //根据包名、类名获取资源定义的图标资源id
    private static int getIconId(String packageName, String className) {
        if(packageName.equals("com.zoulou.dab") && className.equals("com.zoulou.dab.activity.MainActivity"))
        {
            //return R.drawable.ic_dab;
        }

        if(packageName.equals("com.my.bt") && className.equals("com.my.bt.ATBluetoothActivity"))
        {
            return R.drawable.app_blutooth_call;
        }

        if(packageName.equals("com.suding.speedplay") && className.equals("com.suding.speedplay.ui.MainActivity"))
        {
            return R.drawable.app_carplay;
        }

        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.radio.RadioActivity"))
        {
            return R.drawable.app_radio;
        }
        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.my.video.VideoActivity"))
        {
            return R.drawable.ic_video;
        }

        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.audio.MusicActivity"))
        {
            return R.drawable.app_music;
        }

        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.btmusic.BTMusicActivity"))
        {
           return R.drawable.app_blutooth_music;
        }

        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.auxplayer.AUXPlayer"))
        {
            return R.drawable.app_auxin;
        }

        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.frontcamera.FrontCameraActivity"))
        {
            return R.drawable.app_f_camara;
        }

        if(packageName.equals("com.android.settings") && className.equals("com.android.settings.homepage.SettingsHomepageActivity"))
        {
            return R.drawable.app_setting;
        }

        if(packageName.equals("com.anydesk.anydeskandroid") && className.equals("com.anydesk.anydeskandroid.gui.activity.MainActivity"))
        {
            //return R.drawable.apps_anydesk;
        }
        if(packageName.equals("com.my.instructions") && className.equals("com.my.instructions.InstructionsActivity"))
        {
            return R.drawable.app_manual;
        }
        if(packageName.equals("com.android.gallery3d") && className.equals("com.android.gallery3d.app.GalleryActivity"))
        {
            return R.drawable.app_pictures;
        }
        if(packageName.equals("com.SwcApplication") && className.equals("com.SwcApplication.SwcActivity"))
        {
            return R.drawable.app_fk;
        }

        if(packageName.equals("com.android.deskclock") && className.equals("com.android.deskclock.DeskClock"))
        {
            return R.drawable.app_clock;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.CanAirControlActivity"))
        {
            return R.drawable.car_acc;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.MainActivity"))
        {
            return R.drawable.car_3;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.CarInfoActivity"))
        {
            return R.drawable.car_1;
        }

        if(packageName.equals("com.canboxsetting") && className.equals("com.canboxsetting.JeepCarCDPlayerActivity"))
        {
            return R.drawable.car_2;
        }

        if(packageName.equals("com.google.android.apps.maps") && className.equals("com.google.android.maps.MapsActivity"))
        {
            return R.drawable.app_googlemap;
        }

        if(packageName.equals("com.google.android.youtube"))
        {
            return R.drawable.app_youtube;
        }

        if(packageName.equals("com.my.fileexplorer")&& className.equals("com.my.fileexplorer.FileManagerActivity"))
        {
            //return R.drawable.app_1;
        }
        if(packageName.equals("com.android.calculator2")&& className.equals("com.android.calculator2.Calculator"))
        {
            return R.drawable.app_caculator;
        }
        if(packageName.equals("com.android.settings") && className.equals("com.android.settings.Settings"))
        {
            return R.drawable.app_setting;
        }

        if(packageName.equals("com.google.android.googlequicksearchbox") && className.equals("com.google.android.googlequicksearchbox.VoiceSearchActivity"))
        {
            //return R.drawable.app_seach;
        }

        if(packageName.equals("com.android.documentsui") && className.equals("com.android.documentsui.LauncherActivity"))
        {
            return R.drawable.app_foder;
        }

        if(packageName.equals("com.my.filemanager") && className.equals("com.my.filemanager.FileManagerActivity"))
        {
            return R.drawable.app_apk;
        }

        if(packageName.equals("com.android.chrome") && className.equals("com.google.android.apps.chrome.Main"))
        {
            return R.drawable.app_chrome;
        }

        if(packageName.equals("com.eqset") && className.equals("com.eqset.EQActivity"))
        {
            return R.drawable.app_dsp;
        }

        if(packageName.equals("com.car.dvdplayer") && className.equals("com.car.dvdplayer.DVDPlayerActivity"))
        {
            //return R.drawable.app_2;
        }

        if(packageName.equals("com.my.dvr") && className.equals("com.my.dvr.MainActivity"))
        {
            return R.drawable.app_dvr;
        }

        if(packageName.equals("com.google.android.googlequicksearchbox") && className.equals("com.google.android.googlequicksearchbox.SearchActivity"))
        {
            return R.drawable.app_google;
        }

        if(packageName.equals("com.android.calendar") && className.equals("com.android.calendar.AllInOneActivity"))
        {
            return R.drawable.app_calendar;
        }

        if(packageName.equals("com.android.vending") && className.equals("com.android.vending.AssetBrowserActivity"))
        {
            return R.drawable.app_playstore;
        }

        if(packageName.equals("com.android.quicksearchbox") && className.equals("com.android.quicksearchbox.SearchActivity"))
        {
            return R.drawable.app_seach;
        }
        if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.navi.NaviEmptyActivity"))
        {
            return R.drawable.app_navigate;
        }
        return -1;
    }

    public static String getTitle(Context context, String packageName, String className) {

        if(packageName.equals("com.zoulou.dab") || className.equals("com.zoulou.dab.activity.MainActivity"))
        {
            //return null;
            //return context.getString(R.string.dab);
        }

        else if(packageName.equals("com.my.bt") && className.equals("com.my.bt.ATBluetoothActivity"))
        {
            return null;
        }

        else if(packageName.equals("com.suding.speedplay") && className.equals("com.suding.speedplay.ui.MainActivity"))
        {
            //return context.getString(R.string.car_play_connection);
        }

        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.radio.RadioActivity"))
        {
            //return context.getString(R.string.radio);
        }
        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.video.VideoActivity"))
        {
            return null;
        }
        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.audio.MusicActivity"))
        {
            return null;
        }
        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.btmusic.BTMusicActivity"))
        {
            return null;
        }
        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.auxplayer.AUXPlayer"))
        {
            return null;
        }

        else if(packageName.equals("com.octopus.android.carapps") && className.equals("com.octopus.android.carapps.frontcamera.FrontCameraActivity"))
        {
            return null;
        }

        else if(packageName.equals("com.android.settings") && className.equals("com.android.settings.Settings"))
        {
            return null;
        }
        else if(packageName.equals("com.anydesk.anydeskandroid") && className.equals("com.anydesk.anydeskandroid.gui.activity.MainActivity"))
        {
            return null;
        }
        else if(packageName.equals("com.my.instructions") && className.equals("com.my.instructions.InstructionsActivity"))
        {
            return context.getString(R.string.manual);
        }
        else if(packageName.equals("com.android.gallery3d") && className.equals("com.android.gallery3d.app.GalleryActivity"))
        {
            return null;
        }
        else if(packageName.equals("com.SwcApplication") && className.equals("com.SwcApplication.SwcActivity"))
        {
            //return context.getString(R.string.swcapplication);
        }
        else if(packageName.equals("com.android.deskclock") && className.equals("com.android.deskclock.DeskClock"))
        {
            return null;
        }

        if(packageName.equals("com.google.android.apps.maps") && className.equals("com.google.android.maps.MapsActivity"))
        {
            //return context.getString(R.string.navigate);
        }

        {
            return null;
        }
    }

}