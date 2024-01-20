/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher2;

import com.android.launcher.R;

import android.adservices.measurement.MeasurementManager;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import com.ce.view.WinceCEStyleApp;
import com.common.util.AppConfig;
import com.common.util.MachineConfig;
import com.zhuchao.android.fbase.MMLog;

import java.util.HashMap;
import java.util.Objects;

/**
 * Cache of application icons. Icons can be made from any thread.
 */
public class IconCache {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
        public int mBackgroundId;
        public CharSequence contentDescription;
    }

    private static class CacheKey {
        public ComponentName componentName;
        public UserHandle user;

        CacheKey(ComponentName componentName, UserHandle user) {
            this.componentName = componentName;
            this.user = user;
        }

        @Override
        public int hashCode() {
            return componentName.hashCode() + user.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            CacheKey other = (CacheKey) o;
            return other.componentName.equals(componentName)
                    && other.user.equals(user);
        }
    }

    private final Bitmap mDefaultIcon;
    private final LauncherApplication mContext;
    private final PackageManager mPackageManager;
    private final HashMap<CacheKey, CacheEntry> mCache = new HashMap<CacheKey, CacheEntry>(
            INITIAL_ICON_CACHE_CAPACITY);
    private final int mIconDpi;

    public IconCache(LauncherApplication context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();
        // need to set mIconDpi before getting default icon
        mDefaultIcon = makeDefaultIcon();
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon,
                android.os.Process.myUserHandle());
    }

    public Drawable getFullResIcon(Resources resources, int iconId,UserHandle user) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        if (d == null) {
            d = getFullResDefaultActivityIcon();
        }
        return mPackageManager.getUserBadgedIcon(d, user);
    }

    public Drawable getFullResIcon(String packageName, int iconId,
                                   UserHandle user) {
        Resources resources;
        try {
            // TODO: Check if this needs to use the user param if we support
            // shortcuts/widgets from other profiles. It won't work as is
            // for packages that are only available in a different user profile.
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, user);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info, UserHandle user) {
        return getFullResIcon(info.activityInfo, user);
    }

    public Drawable getFullResIcon(ActivityInfo info, UserHandle user) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }

        if (resources != null)
        {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, user);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1), Math.max(d.getIntrinsicHeight(), 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ApplicationInfo application,LauncherActivityInfo info, HashMap<Object, CharSequence> labelCache)
    {
        synchronized (mCache)
        {
            CacheEntry entry = cacheLocked2(application.componentName, info, labelCache, info.getUser());
            MMLog.d(TAG,"getTitleAndIcon ComponentName="+application.componentName + ",entry.title=" + entry.title);

            //WinceCEStyleApp.updateLabel(application.componentName, entry.title);
            application.title = entry.title;
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
        }
    }

    public Bitmap getIcon(Intent intent, UserHandle user) {
        synchronized (mCache) {
            LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            final LauncherActivityInfo launcherActInfo = launcherApps.resolveActivity(intent, user);
            ComponentName component = intent.getComponent();
            MMLog.d(TAG,"getIcon intent="+ intent.toString());

            if (launcherActInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, launcherActInfo, null,user);
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, LauncherActivityInfo info, HashMap<Object, CharSequence> labelCache)
    {
        synchronized (mCache) {
            if (info == null || component == null) {
                return null;
            }
            ///MMLog.d(TAG,"getIcon component="+ component.toString());
            CacheEntry entry = cacheLocked(component, info, labelCache,info.getUser());
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }

    private final String[] needRoundPkgName = new String[]{
            "com.google.android.gm",
            "com.android.chrome",
            "com.android.vending",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.maps",
            "com.google.android.gms",
            "com.android.settings",
            "com.google.android.youtube"};

    private boolean ifNeedToRound(String pkgName) {
        if (pkgName != null) {
            for (String s : needRoundPkgName) {
                if (pkgName.equals(s))
                    return true;
            }
        }
        return false;
    }

    private CacheEntry cacheLocked(ComponentName componentName,LauncherActivityInfo info,HashMap<Object, CharSequence> labelCache, UserHandle user)
    {
        CacheKey cacheKey = new CacheKey(componentName, user);
        CacheEntry entry = mCache.get(cacheKey);
        //MMLog.d(TAG,"cacheLocked() componentName="+componentName.getPackageName() +","+ componentName.getClassName());

        if (entry == null)
        {
            entry = new CacheEntry();
            mCache.put(cacheKey, entry);
            ComponentName key = info.getComponentName();

            if (labelCache != null && labelCache.containsKey(key)) {
                entry.title = Objects.requireNonNull(labelCache.get(key)).toString();
            }
            else
            {
                entry.title = info.getLabel().toString();
                if (labelCache != null) {
                    labelCache.put(key, entry.title);
                }
            }

            ///if (entry.title == null) {
            ///    entry.title = info.getComponentName().getShortClassName();
            ///}

            entry.contentDescription = mPackageManager.getUserBadgedLabel(entry.title, user);
            Drawable d = LauncherIconTheme.getIconDrawable(mContext,componentName.getPackageName(),componentName.getClassName());
            String title = LauncherIconTheme.getTitle(mContext,componentName.getPackageName(),componentName.getClassName());
            if(title != null)
                entry.title = title;

            if (MachineConfig.VALUE_SYSTEM_UI_KLD5.equals(Utilities.mSystemUI)) {
                d = info.getBadgedIcon(mIconDpi);
                entry.icon = Utilities.createIconBitmap(d, mContext, R.drawable.app_wrap);
            }
            else if (MachineConfig.VALUE_SYSTEM_UI_KLD11_200.equals(Utilities.mSystemUI) && d != null) {
                //d = LauncherIconTheme.getIconDrawable(mContext,componentName.getPackageName(),componentName.getClassName());
                entry.icon = Utilities.createIconBitmap(d, mContext);
                if (labelCache != null)
                   labelCache.put(key, entry.title);
            }
            else
            {
                boolean isNeedBg = false;
                if (Utilities.mSystemUI != null && !Utilities.mSystemUI.equals(MachineConfig.VALUE_SYSTEM_UI_KLD11_200))
                {
                    d = getIconBackground(key.getPackageName(),key.getClassName());
                    if (d == null && !MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(Utilities.mSystemUI)
                            && !MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(Utilities.mSystemUI)) {
                        isNeedBg = true;
                    }
                    ///if("com.ak.servicepoint".equals(key.getPackageName())){
                    ///isNeedBg = false;
                    ///}
                }
                else
                {
                    isNeedBg = !AppConfig.isNoNeedLauncherBackground(key.getPackageName(), key.getClassName());
                }

                if (d == null) {
                    d = info.getBadgedIcon(mIconDpi);
                }

                if ("com.eqset".equals(key.getPackageName())) {
                    if (Utilities.mIsDSP) {
                        d = getIconBackground(key.getPackageName(),key.getClassName());
                        isNeedBg = true;
                    }
                }

                entry.icon = Utilities.createIconBitmap(d, mContext, isNeedBg);

                if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(Utilities.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(Utilities.mSystemUI))
                {
                    String pkgName = key.getPackageName();
                    if (ifNeedToRound(pkgName)) {
                        entry.icon = ResourceUtil.toRoundBitmap(entry.icon);
                    }
                }
            }
        }
        return entry;
    }

    private CacheEntry cacheLocked2(ComponentName componentName,LauncherActivityInfo info,HashMap<Object, CharSequence> labelCache, UserHandle user)
    {
        CacheKey cacheKey = new CacheKey(componentName, user);
        CacheEntry entry = mCache.get(cacheKey);
        //MMLog.d(TAG,"cacheLocked() componentName="+componentName.getPackageName() +","+ componentName.getClassName());

        if (entry == null)
        {
            entry = new CacheEntry();
            mCache.put(cacheKey, entry);
            ComponentName key = info.getComponentName();

            if (labelCache != null && labelCache.containsKey(key)) {
                entry.title = Objects.requireNonNull(labelCache.get(key)).toString();
            }
            else
            {
                entry.title = info.getLabel().toString();
                if (labelCache != null) {
                    labelCache.put(key, entry.title);
                }
            }

            entry.contentDescription = mPackageManager.getUserBadgedLabel(entry.title, user);
            Drawable d = LauncherIconTheme.getAppIconDrawable(mContext,componentName.getPackageName(),componentName.getClassName());
            //String title = LauncherIconTheme.getTitle(mContext,componentName.getPackageName(),componentName.getClassName());
            if (d != null) {
                entry.icon = Utilities.createIconBitmap(d, mContext);
                if (labelCache != null)
                    labelCache.put(key, entry.title);
            }
            else
            {
                //MMLog.d(TAG,"NO CUSTOMIZE ICON!!!!!!!!"+componentName.getClassName());
                boolean isNeedBg =  false;// !AppConfig.isNoNeedLauncherBackground(key.getPackageName(), key.getClassName());
                d = info.getBadgedIcon(mIconDpi);
                entry.icon = Utilities.createIconBitmap(d, mContext, isNeedBg);
            }
        }
        return entry;
    }
    private Drawable getIconBackground(String packageName, String className) {
        String s = null;
        Drawable d = null;
        MMLog.d(TAG,"getIconBackground()");
        if ("com.car.ui".equals(packageName)) {
            if ("com.my.navi.NaviEmptyActivity".equals(className)) {
                s = "navi";
            } else if ("com.my.audio.MusicActivity".equals(className)) {
                s = "music";
            } else if ("com.my.dvd.DVDPlayer".equals(className)) {
                s = "dvd";
            } else if ("com.my.radio.RadioActivity".equals(className)) {
                s = "radio";
            } else if ("com.my.video.VideoActivity".equals(className)) {
                s = "video";
            } else if ("com.my.auxplayer.AUXPlayer".equals(className)) {
                s = "auxin";
            } else if ("com.my.btmusic.BTMusicActivity".equals(className)) {
                s = "bt_music";
            } else if ("com.my.frontcamera.FrontCameraActivity".equals(className)
                    || "com.my.frontcamera.FrontCameraActivity4".equals(className)) {
                s = "f_camera";
            } else if ("com.my.tv.TVActivity".equals(className)) {
                s = "tv";
            }

        } else if ("com.my.bt".equals(packageName)) {
            if ("com.my.bt.ATBluetoothActivity".equals(className)) {
                s = "bt";
            } else if ("com.my.bt.VoiceControlActivity".equals(className)) {
                s = "bt_voice";
            }
        } else if ("com.android.gallery3d".equals(packageName)) {
            s = "gallery";
        } else if ("com.eqset".equals(packageName)) {
            //Log.d("ffk", ":" + Utilities.mIsDSP);
            if (Utilities.mIsDSP) {
                d = mContext.getDrawable(R.drawable.dsp);
                return d;
            } else {
                s = "eq";
            }
        } else if ("com.android.browser".equals(packageName) ||
                "com.android.chrome".equals(packageName)) {
            s = "browse";
        } else if ("com.my.dvr".equals(packageName)) {
            if ("com.my.dvr.MainActivity".equals(className)) {
                s = "dvr";
            }
        } else if ("com.SwcApplication".equals(packageName)) {
            if ("com.SwcApplication.SwcActivity".equals(className)) {
                s = "keystudy";
            }
        } else if ("com.android.settings".equals(packageName)) {
            s = "set";
        } else if ("com.google.android.gm.ConversationListActivityGmail".equals(className)) {
            s = "gmail";
        } else if ("com.google.android.gms.app.settings.GoogleSettingsActivity".equals(className)) {
            s = "gsettings";
        } else if ("com.android.vending.AssetBrowserActivity".equals(className)) {
            s = "gvending";
        } else if ("com.google.android.googlequicksearchbox.VoiceSearchActivity".equals(className)) {
            s = "gvoice";
        } else if ("com.google.android.googlequicksearchbox.SearchActivity".equals(className)) {
            s = "gsearch";
        } else if ("com.my.instructions.InstructionsActivity".equals(className)) {
            s = "instructions";
        } else if ("com.estrongs.android.pop.view.FileExplorerActivity".equals(className)) {
            s = "file_explor";
        } else if ("com.android.calendar.AllInOneActivity".equals(className)) {
            s = "calendar";
        } else if ("com.android.calculator2.Calculator".equals(className)) {
            s = "calc";
        } else if ("com.google.android.maps.MapsActivity".equals(className)) {
            s = "gmap";
        } else if ("com.car.dvdplayer".equals(packageName)) {
            s = "dvd";
        }

        if (s != null) {
            String paramter_s = Utilities.mIconPath + s + ".png";
            MMLog.d(TAG,"GET Icon file:"+paramter_s);

            d = Drawable.createFromPath(paramter_s);
            if ((MachineConfig.VALUE_SYSTEM_UI40_KLD90.equals(Utilities.mSystemUI) ||
                    MachineConfig.VALUE_SYSTEM_UI_9813.equals(Utilities.mSystemUI)) &&
                    (Launcher.ICON_TYPE_PARAMTER_ICON != Launcher.mIconType)) {
                if (d == null) {
                    if (Utilities.mDark && Utilities.mDarkSwitch == 1) {
                        s += "_d";
                    }
                    int j = mContext.getResources().getIdentifier(s, "drawable", mContext.getPackageName());
                    if (j != 0) {
                        d = mContext.getDrawable(j);
                    }
                }
            }


            if (d instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) d;
                bitmapDrawable.setTargetDensity(mContext.getResources().getDisplayMetrics());
            }
        }

        return d;
    }
}
