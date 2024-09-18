package com.ce.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher.R;
import com.android.launcher2.FixScreenShortcut;
import com.android.launcher2.Utilities;
import com.android.launcher2.Launcher;

import com.common.utils.AppConfig;
import com.common.utils.SettingProperties;
import com.common.utils.UtilCarKey;
import com.common.utils.Util;

public class WinceCEStyleApp {

    private static final String TAG = "WinceCEStyleApp";
    private Activity mContext;
    // all applications
    private GridView mGrid;
    private static ArrayList<ApplicationInfo> mApplications;

    static WinceCEStyleApp mThis;

    public static WinceCEStyleApp getInstance(Activity ac) {
        mThis = new WinceCEStyleApp();
        mThis.init(ac);
        return mThis;
    }

    private void init(Activity ac) {
        mContext = ac;
        initView();
        if (isGridApp()) {
            initAllApplications();
        }

    }

    private boolean isGridApp() {
        if (mContext.findViewById(R.id.ce_all_apps) == null) {
            return false;
        } else {
            return true;
        }
    }

    private void showAllApp(boolean b) {
        if (isGridApp()) {
            if (mGrid == null) {
                initAllApplications();
            }
            mGrid.setVisibility(b ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isAllAppShow() {
        if (mCEView != null && mCEView.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    HorizontalScrollView mHorizontalScrollView;
    private int ICON_SIZE = 204;

    private void initView() {
        ICON_SIZE = (int) mContext.getResources().getDimension(R.dimen.ce_application_icon_width);
        Log.d("eef", ":" + ICON_SIZE);
        mAndroidView = mContext.findViewById(R.id.drag_layer);
        mCEView = mContext.findViewById(R.id.ce_android);

        mCEView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub
                if (!mFixWallpaper) {
                    final Intent pickWallpaper = new Intent(
                            Intent.ACTION_SET_WALLPAPER);
                    Intent chooser = Intent.createChooser(
                            pickWallpaper,
                            mContext.getResources().getText(
                                    R.string.chooser_wallpaper));

                    mContext.startActivityForResult(chooser,
                            REQUEST_PICK_WALLPAPER);
                }
                return false;
            }
        });

        ((ImageView) mContext.findViewById(R.id.ce_icon_android))
                .setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        hide();
                    }
                });

        updateAppHide();
    }

    private void initAllApplications() {
        loadApplications(true);
        bindApplications();
        bindButtons();

        IntentFilter filter;
        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mApplicationsReceiver, filter);

        // test

        mHorizontalScrollView = (HorizontalScrollView) mContext
                .findViewById(R.id.ce_all_apps_scrolview);

        ((ImageView) mContext.findViewById(R.id.ce_next))
                .setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        next();
                    }
                });
        ((ImageView) mContext.findViewById(R.id.ce_prev))
                .setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        prev();
                    }
                });

    }

    private static final int REQUEST_PICK_WALLPAPER = 10;

    private void bindApplications() {
        if (mGrid == null) {
            mGrid = (GridView) mContext.findViewById(R.id.ce_all_apps);
        }
        mGrid.setAdapter(new ApplicationsAdapter(mContext, mApplications));
        mGrid.setSelection(0);

        // if (mApplicationsStack == null) {
        // mApplicationsStack = (ApplicationsStackLayout)
        // findViewById(R.id.faves_and_recents);
        // }

        LinearLayout mLayout = (LinearLayout) mContext
                .findViewById(R.id.all_apps_layout);
        ViewGroup.LayoutParams lp;
        lp = mLayout.getLayoutParams();
        lp.width = mApplications.size() * ICON_SIZE;
        mLayout.setLayoutParams(lp);
    }

    /**
     * GridView adapter to show the list of all installed applications.
     */
    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
        private Rect mOldBounds = new Rect();

        public ApplicationsAdapter(Context context,
                                   ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ApplicationInfo info = mApplications.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = mContext.getLayoutInflater();
                convertView = inflater.inflate(R.layout.ce_application, parent,
                        false);
            }

            // Drawable icon = info.icon;

            // if (!info.filtered) {
            // // final Resources resources = getContext().getResources();
            // int width = 142;// (int)
            // // resources.getDimension(android.R.dimen.app_icon_size);
            // int height = 142;// (int)
            // // resources.getDimension(android.R.dimen.app_icon_size);
            //
            // final int iconWidth = icon.getIntrinsicWidth();
            // final int iconHeight = icon.getIntrinsicHeight();
            //
            // if (icon instanceof PaintDrawable) {
            // PaintDrawable painter = (PaintDrawable) icon;
            // painter.setIntrinsicWidth(width);
            // painter.setIntrinsicHeight(height);
            // }
            //
            // if (width > 0 && height > 0
            // && (width < iconWidth || height < iconHeight)) {
            // final float ratio = (float) iconWidth / iconHeight;
            //
            // if (iconWidth > iconHeight) {
            // height = (int) (width / ratio);
            // } else if (iconHeight > iconWidth) {
            // width = (int) (height * ratio);
            // }
            //
            // final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
            // Bitmap.Config.ARGB_8888
            // : Bitmap.Config.RGB_565;
            // final Bitmap thumb = Bitmap.createBitmap(width, height, c);
            // final Canvas canvas = new Canvas(thumb);
            // canvas.setDrawFilter(new PaintFlagsDrawFilter(
            // Paint.DITHER_FLAG, 0));
            // // Copy the old bounds to restore them later
            // // If we were to do oldBounds = icon.getBounds(),
            // // the call to setBounds() that follows would
            // // change the same instance and we would lose the
            // // old bounds
            // mOldBounds.set(icon.getBounds());
            // icon.setBounds(0, 0, width, height);
            // icon.draw(canvas);
            // icon.setBounds(mOldBounds);
            // icon = info.icon = new BitmapDrawable(thumb);
            // info.filtered = true;
            // }
            // }

            Bitmap bmp;
            Drawable d = getIconBackground(info.intent.getComponent()
                    .getPackageName(), info.intent.getComponent()
                    .getClassName());

            if (d == null) {
                bmp = Utilities.createIconBitmapWinCEUI(info.icon, mContext,
                        R.drawable.app_wrap);
            } else {
                bmp = Utilities.createIconBitmapWinCEUI(d, mContext,
                        R.drawable.app_wrap);
            }

            Drawable icon = new BitmapDrawable(bmp);

            final TextView textView = (TextView) convertView
                    .findViewById(R.id.ce_label);
            textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null,
                    null);
            textView.setText(info.title);

            return convertView;
        }
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }

        PackageManager manager = mContext.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(
                mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            if (mApplications == null) {
                mApplications = new ArrayList<ApplicationInfo>(count);
            }
            mApplications.clear();
            clearFirstApp();
            for (int i = 0; i < count; i++) {
                ApplicationInfo application = new ApplicationInfo();
                ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(manager);
                application.title = FixScreenShortcut.getAppAlternativeTitle(mContext, new ComponentName(info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name), application.title);

                application.setActivity(new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.icon = info.activityInfo.loadIcon(manager);

                if (!AppConfig.isHidePackage(info.activityInfo.name)) {

                    int index = getAppIndex(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name);
                    Log.d("ee", index + ":" + mApplications.size() + ":" + info.activityInfo.name + "!!!!!!!!!!!!!!");

                    if (index >= 0 && index < mFirstApp.length) {
                        mFirstApp[index] = application;
                    } else {
                        mApplications.add(application);
                    }

                }
            }
        }
        addFirstApp();
    }

    private void clearFirstApp() {
        for (int j = mFirstApp.length - 1; j >= 0; --j) {
            mFirstApp[j] = null;
        }
    }

    private void addFirstApp() {
        for (int j = mFirstApp.length - 1; j >= 0; --j) {
            ApplicationInfo application = mFirstApp[j];
            if (application != null) {
                mApplications.add(0, application);
            }
        }
    }

    private ApplicationInfo[] mFirstApp = new ApplicationInfo[FISRT_APPLICATION.length];
    private final static ComponentName[] FISRT_APPLICATION = {
            new ComponentName("com.car.ui", "com.my.navi.NaviEmptyActivity"),
            new ComponentName("com.car.ui", "com.my.radio.RadioActivity"),
            new ComponentName("com.my.bt", "com.my.bt.ATBluetoothActivity"),
            new ComponentName("com.car.ui", "com.my.audio.MusicActivity"),
            new ComponentName("com.car.ui", "com.my.video.VideoActivity"),
            new ComponentName("com.android.settings", "com.android.settings.Settings"),
            new ComponentName("com.car.ui", "com.my.dvd.DVDPlayer"),
            new ComponentName("com.car.ui", "com.my.tv.TVActivity"),
            new ComponentName("com.car.ui", "com.my.auxplayer.AUXPlayer"),
            new ComponentName("com.car.ui", "com.my.btmusic.BTMusicActivity"),
            new ComponentName("com.eqset", "com.eqset.EQActivity"),
            new ComponentName("com.car.ui", "com.my.tv.TVActivity")};

    private int getAppIndex(String packName, String className) {

        for (int i = 0; i < FISRT_APPLICATION.length; ++i) {
            if (FISRT_APPLICATION[i].getClassName().equals(className)
                    && FISRT_APPLICATION[i].getPackageName().equals(packName)) {
                return i;
            }
        }
        return -1;
    }

    private Drawable getIconBackground(String packageName, String className) {
        String s = null;
        Drawable d = null;
        //MMLog.log(TAG,"getIconBackground "+className);


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
            } else if ("com.my.frontcamera.FrontCameraActivity"
                    .equals(className)
                    || "com.my.frontcamera.FrontCameraActivity4"
                    .equals(className)) {
                s = "f_camera";
            } else if ("com.my.tv.TVActivity".equals(className)) {
                s = "tv";
            }

        } else if ("com.my.bt".equals(packageName)) {
            s = "bt";
        } else if ("com.android.gallery3d".equals(packageName)) {
            s = "gallery";
        } else if ("com.eqset".equals(packageName)) {
            s = "eq";
        } else if ("com.android.browser".equals(packageName)) {
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
            // if ("com.android.settings.SwcActivity".equals(className)) {
            s = "set";
            // }
        }
        if (s != null) {
            s = "/mnt/paramter/icon_ce/" + s + ".png";
            d = Drawable.createFromPath(s);
            if (d == null) {
                s = "/mnt/paramter/icon/" + s + ".png";
                d = Drawable.createFromPath(s);
            }

            if (d instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) d;
                bitmapDrawable.setTargetDensity(mContext.getResources()
                        .getDisplayMetrics());
            }
        }

        // Log.d("allen", className + ":" + s + ":" + d);
        return d;

    }

    /**
     * Starts the selected activity/application in the grid view.
     */
    private class ApplicationLauncher implements
            AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView parent, View v, int position,
                                long id) {
            ApplicationInfo app = (ApplicationInfo) parent
                    .getItemAtPosition(position);

            if (!Launcher.isNaviFakeApp(app.intent)) {
                mContext.startActivity(app.intent);
            } else {
                UtilCarKey.doKeyGps(mContext);
            }
        }
    }

    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();

    /**
     * Receives notifications when applications are added/removed.
     */
    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isGridApp()) {
                loadApplications(false);
                bindApplications();
            }
        }
    }

    /**
     * Binds actions to the various buttons.
     */
    private void bindButtons() {

        mGrid.setOnItemClickListener(new ApplicationLauncher());
    }

    private View mCEView;
    private View mAndroidView;

    public void show(boolean first) {

//		if (mLoader != null
//				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
//			return;
//		}

        if (mCEView != null) {
            mCEView.setVisibility(View.VISIBLE);
            if (mCEStyle == 2) {
                View v = mContext.findViewById(R.id.ce_icon_android);
                if (v != null) {
                    v.setEnabled(false);
                }
            }
        }
        if (mAndroidView != null) {
            mAndroidView.setVisibility(View.GONE);
        }
        if (!first) {
            updateWallpaper(mContext, true);
        }
        if (getData(SAVE_DATA_CE) != 1) {
            saveData(SAVE_DATA_CE, 1);
        }
    }

    private static String SAVE_DATA = "WinceCEStyleApp";
    private static String SAVE_DATA_CE = "ce";

    private void saveData(String s, int v) {
        SharedPreferences.Editor sharedata = mContext.getSharedPreferences(SAVE_DATA, 0)
                .edit();

        sharedata.putInt(s, v);
        sharedata.commit();
        Util.sudoExec("sync");
    }

    private int getData(String s) {
        SharedPreferences sharedata = mContext.getSharedPreferences(SAVE_DATA, 0);
        return sharedata.getInt(s, 0);
    }

    public void recover() {
        if (getData(SAVE_DATA_CE) == 1) {
            show(true);
        }
    }

    public void show() {
        show(false);
    }

    private View mLauncherView;

    public void showLauncherView(View v) {
        show();
        mLauncherView = v;
        try {
            v.setBackgroundResource(R.drawable.default_ce_screen);
        } catch (Exception e) {

        }
    }

    public void hide() {
        if (mLauncherView != null) {
            mLauncherView.setBackground(null);
        }
//		if (mLoader != null
//				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
//			return;
//		}

        if (mCEView != null) {
            mCEView.setVisibility(View.GONE);
        }
        if (mAndroidView != null) {
            mAndroidView.setVisibility(View.VISIBLE);
        }
        updateWallpaper(mContext, false);

        saveData(SAVE_DATA_CE, 0);
    }

    private boolean isEquals(Bitmap b1, Bitmap b2) {
        if (b1 == null || b2 == null) {
            return false;
        }
        // 先判断宽高是否一致，不一致直接返回false
        if (b1.getWidth() == b2.getWidth() && b1.getHeight() == b2.getHeight()) {
            int xCount = b1.getWidth();
            int yCount = b1.getHeight();
            for (int x = 0; x < xCount; x++) {
                for (int y = 0; y < yCount; y++) {
                    // 比较每个像素点颜色
                    if (b1.getPixel(x, y) != b2.getPixel(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    Drawable mOldWallpaper; // = wpm.getDrawable();

    private void saveOldWallpaper(Context context, boolean ceUI) {

        // Log.d("ff", "saveOldWallpaper 1111111");
        // WallpaperManager wpm = WallpaperManager.getInstance(context);

        Bitmap bmpNow = null;
        Bitmap bmpOld = null;
        if (mOldWallpaper != null) {
            if (mOldWallpaper instanceof BitmapDrawable) {
                BitmapDrawable new_name = (BitmapDrawable) mOldWallpaper;
                bmpNow = new_name.getBitmap();
            }
        }

        String path;

        if (ceUI) {
            path = SettingProperties.PATH_CE_WALLPAPER
                    + SettingProperties.PATH_DEFAULT_CE_WALLPAPER;
        } else {
            path = SettingProperties.PATH_CE_WALLPAPER
                    + SettingProperties.PATH_DEFAULT_WALLPAPER0;
        }
        File f = new File(path);
        if (f.exists()) {
            bmpOld = BitmapFactory.decodeFile(path);
        }

        // Log.d("ff", "saveOldWallpaper 2222222");
        boolean equal = isEquals(bmpNow, bmpOld);

        // Log.d("ff", "saveOldWallpaper 33333333::"+path);
        if (!equal) {
            checkDir();
            saveBmpFile(bmpNow, path);
        }
        // Log.d("ff", "isEquals(bmpNow, bmpOld):"+equal);
    }

    private void saveBmpFile(Bitmap img, String path) {
        try {
            OutputStream os = new FileOutputStream(path);
            img.compress(CompressFormat.PNG, 100, os);
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "save:", e);
        }
    }

    private void checkDir() {
        try {
            File f = (new File(SettingProperties.PATH_CE_WALLPAPER));
            if (!f.exists()) {
                f.mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "checkDir:", e);
        }
    }

    private boolean mFixWallpaper = false;
    private boolean mCEUI = false;

    public void setFixWallPaper(boolean fix) {
        mFixWallpaper = fix;
    }

    private void updateWallpaper(Context context, boolean ceUI) {

        // Log.d("ff", "updateWallpaper1:");
        mCEUI = ceUI;
        if (mFixWallpaper) {
            String path = SettingProperties.PATH_WALLPAPER
                    + SettingProperties.PATH_DEFAULT_CE_WALLPAPER;

            Drawable d = Drawable.createFromPath(path);

            if (d != null) {
                mCEView.setBackground(d);
            }

        } else {
//			mLoader = (WallpaperLoader) new WallpaperLoader().execute();
        }

        // Log.d("ff", "updateWallpaper2:");
    }

    public int mCEStyle = 0;

    private void updateWallpaperAsync(Context context, boolean ceUI) {
        String path;
        Bitmap bmp = null;
        WallpaperManager wpm = WallpaperManager.getInstance(context);

        WallpaperInfo wi = wpm.getWallpaperInfo();
        if (wi != null) {
            return;
        }

        Log.d("ff", "updateWallpaperAsync:" + mCEStyle);
        mOldWallpaper = wpm.getDrawable();

        try {
            if (ceUI) {
                if (mCEStyle != 2) {
                    SettingProperties.setIntProperty(context,
                            SettingProperties.KEY_CE_STYLE, 1);
                }

                // path = SystemConfig.getProperty(context,
                // SystemConfig.KEY_CE_STYLE_WALLPAPER_NAME);
                path = SettingProperties.PATH_CE_WALLPAPER
                        + SettingProperties.PATH_DEFAULT_CE_WALLPAPER;

                File f = new File(path);
                if (!f.exists()) {
                    path = SettingProperties.PATH_WALLPAPER
                            + SettingProperties.PATH_DEFAULT_CE_WALLPAPER;
                } else {
                    // path = SystemConfig.PATH_WALLPAPER
                    // + path;
                    bmp = BitmapFactory.decodeFile(path);
                }

                // Log.d("ff", "updateWallpaper11:"+path+":"+bmp);

                if (bmp == null) {
                    bmp = BitmapFactory.decodeFile(path);
                }

                // Log.d("ff", "updateWallpaper22:"+path+":"+bmp);
                if (bmp != null) {
                    wpm.setBitmap(bmp);
                }

            } else {
                if (mCEStyle != 2) {
                    SettingProperties.setIntProperty(context, SettingProperties.KEY_CE_STYLE, 0);
                }
                /// path = SystemConfig.getProperty(context,
                /// SystemConfig.KEY_SCREEN0_WALLPAPER_NAME);
                path = SettingProperties.PATH_CE_WALLPAPER + SettingProperties.PATH_DEFAULT_WALLPAPER0;

                File f = new File(path);
                if (!f.exists()) {
                    path = SettingProperties.PATH_WALLPAPER + SettingProperties.PATH_DEFAULT_WALLPAPER0;
                } else {
                    /// path = SystemConfig.PATH_WALLPAPER
                    /// + path;
                    bmp = BitmapFactory.decodeFile(path);
                }

                if (bmp == null) {
                    bmp = BitmapFactory.decodeFile(path);
                }

                if (bmp != null) {
                    Log.d(TAG, "wpm.setBitmap:" + path);
                    wpm.setBitmap(bmp);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception:" + e);
        }

        saveOldWallpaper(context, !ceUI);
    }

    private void next() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        if (mHorizontalScrollView.getScrollX() < mApplications.size()
                * ICON_SIZE) {
            int x;
            if (dm.widthPixels == 1024) {
                x = dm.widthPixels + mHorizontalScrollView.getScrollX() - 5;
            }
            if (dm.widthPixels == 1280) {
                x = dm.widthPixels + mHorizontalScrollView.getScrollX() - 2;
            } else /* if(dm.widthPixels == 800) */ {
                x = dm.widthPixels + mHorizontalScrollView.getScrollX() + 5;
            }

            // mHorizontalScrollView.scrollTo(x, 0);
            mHorizontalScrollView.smoothScrollTo(x, 0);
        }
    }

    private void prev() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

        if (mHorizontalScrollView.getScrollX() > 0) {
            int x;
            if (dm.widthPixels == 1024) {
                x = mHorizontalScrollView.getScrollX() - dm.widthPixels + 5;
            } else/* if(dm.widthPixels == 800) */ {
                x = mHorizontalScrollView.getScrollX() - dm.widthPixels + 5;
            }
            // mHorizontalScrollView.scrollTo(x, 0);
            mHorizontalScrollView.smoothScrollTo(x, 0);
            // mHorizontalScrollView.
        }
    }

    public static void forceReload() {
        if (mThis != null) {
            if (mThis.isGridApp()) {
                mThis.loadApplications(false);
                mThis.bindApplications();
            } else {
                mThis.updateAppHide();
            }
        }
    }

    private void updateAppHide() {
        if (AppConfig.isHidePackage("com.my.dvd.DVDPlayer") && !AppConfig.isUSBDvd()) {
            setViewVisible(mContext.findViewById(R.id.button_dvd), View.GONE);
        } else {
            setViewVisible(mContext.findViewById(R.id.button_dvd), View.VISIBLE);
        }
    }

    private void setViewVisible(View v, int visible) {
        if (v != null) {
            v.setVisibility(visible);
        }
    }

    private WallpaperLoader mLoader;

    class WallpaperLoader extends AsyncTask<Void, Integer, Integer> {
        WallpaperLoader() {
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isCancelled())
                return 0;
            updateWallpaperAsync(mContext, mCEUI);
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    //
    final static CEButtonInfo[] mCEButtonInfo = {
            new CEButtonInfo("com.eqset/com.eqset.EQActivity", R.id.button_eq),
            new CEButtonInfo("com.my.filemanager/com.my.filemanager.FileManagerActivity", R.id.button_apk),
            new CEButtonInfo("com.android.calculator2/com.android.calculator2.Calculator", R.id.button_calc),
            new CEButtonInfo("com.android.deskclock/com.android.deskclock.DeskClock", R.id.button_clock),
            new CEButtonInfo("com.car.ui/com.my.audio.MusicActivity", R.id.button_music),
            new CEButtonInfo("com.car.ui/com.my.navi.NaviEmptyActivity", R.id.button_gps),
            new CEButtonInfo("com.car.ui/com.my.radio.RadioActivity", R.id.button_radio),
            new CEButtonInfo("com.android.settings/com.android.settings.Settings", R.id.button_settings),
            new CEButtonInfo("com.SwcApplication/com.SwcApplication.SwcActivity", R.id.button_steer),
            new CEButtonInfo("com.estrongs.android.pop/com.estrongs.android.pop.view.FileExplorerActivity", R.id.button_filebrowse),
            new CEButtonInfo("com.my.fileexplorer/com.my.fileexplorer.FileManagerActivity", R.id.button_filebrowse),
            new CEButtonInfo("com.google.android.gm/com.google.android.gm.ConversationListActivityGmail", R.id.button_gmail),
            new CEButtonInfo("com.google.android.gm.lite/com.google.android.gm.ConversationListActivityGmail", R.id.button_gmail),
            new CEButtonInfo("com.adobe.reader/com.adobe.reader.AdobeReader", R.id.button_adbo),
            new CEButtonInfo("com.car.ui/com.my.dvd.DVDPlayer", R.id.button_dvd),
            new CEButtonInfo("com.android.browser/com.android.browser.BrowserActivity", R.id.button_browse), //5.1
            new CEButtonInfo("com.android.chrome/com.google.android.apps.chrome.Main", R.id.button_browse), //8.0
            new CEButtonInfo("com.android.calendar/com.android.calendar.AllInOneActivity", R.id.button_cale),
            new CEButtonInfo("com.car.ui/com.my.auxplayer.AUXPlayer", R.id.button_aux),
            new CEButtonInfo("com.car.ui/com.my.video.VideoActivity", R.id.button_video),
            new CEButtonInfo("com.my.bt/com.my.bt.ATBluetoothActivity", R.id.button_bluetooth),
            new CEButtonInfo("com.android.gallery3d/com.android.gallery3d.app.GalleryActivity", R.id.button_gallery),
    };

    public boolean openApp(int id) {
        for (CEButtonInfo c : mCEButtonInfo) {
            if (id == c.mId) {
                try {
                    Intent it = new Intent();
                    it.setComponent(c.mComponentName);
                    mContext.startActivity(it);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    public void doUpdateLabel(ComponentName cn, String l) {
        try {
            for (CEButtonInfo c : mCEButtonInfo) {
                if (cn.getClassName().equals(c.mComponentName.getClassName())
                        && cn.getPackageName().equals(c.mComponentName.getPackageName()))
				{
                    View v = mContext.findViewById(c.mId);
                    if (v instanceof Button) {
                        Button new_name = (Button) v;
                        new_name.setText(l);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void updateLabel(ComponentName cn, String l) {
        if (mThis != null) {
            mThis.doUpdateLabel(cn, l);
        }
    }
}
