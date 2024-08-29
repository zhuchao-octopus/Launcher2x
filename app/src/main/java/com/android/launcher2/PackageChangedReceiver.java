package com.android.launcher2;

import java.util.List;
import java.util.Objects;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.android.launcher2.FixScreenShortcut.FixScreenShortcutData;
import com.common.utils.MachineConfig;
import com.zhuchao.android.fbase.MMLog;

public class PackageChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "PackageChangedReceiver";
    private static FixScreenShortcut mFixScreenShortcut = null;

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String packageName = Objects.requireNonNull(intent.getData()).getSchemeSpecificPart();

        if (packageName == null || packageName.length() == 0) {
            // they sent us a bad intent
            return;
        }

        if (!Objects.equals(intent.getAction(), "android.intent.action.PACKAGE_ADDED")) {

            LauncherApplication app = (LauncherApplication) context.getApplicationContext();
            WidgetPreviewLoader.removeFromDb(app.getWidgetPreviewCacheDb(), packageName);

        }

        if (Objects.equals(intent.getAction(), "android.intent.action.PACKAGE_ADDED") || Objects.equals(intent.getAction(), "android.intent.action.PACKAGE_REMOVED")) {

            //			String s = MachineConfig
            //					.getPropertyOnce(MachineConfig.KEY_GPS_PACKAGE);
            //
            //			if (s != null && packageName.equals(s)) {
            //				Launcher.initSettings();
            //			}
        }

        //ww+
        MMLog.d(TAG,"onReceive " + packageName);
        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED") || intent.getAction().equals("android.intent.action.PACKAGE_REPLACED")) {
            try {
                if (Utilities.mSystemUI != null) {
                    if (mFixScreenShortcut == null) mFixScreenShortcut = FixScreenShortcut.getInstance();
                    FixScreenShortcutData fss = null;
                    switch (Utilities.mSystemUI) {
                        case MachineConfig.VALUE_SYSTEM_UI20_RM10_1:
                            if (ResourceUtil.sw == 340) {
                                fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_RM10_1_1280_480);
                            } else {
                                fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_RM10_1);
                            }
                            break;
                        case MachineConfig.VALUE_SYSTEM_UI21_RM10_2:
                            if (ResourceUtil.sw == 346) {
                                fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_RM10_2_1280_480);
                            } else {
                                fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_RM10_2);
                            }
                            break;
                        case MachineConfig.VALUE_SYSTEM_UI21_RM12:
                            fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_RM12);
                            break;
                        default:
                            fss = mFixScreenShortcut.getFixScreenShortcutData(packageName, FixScreenShortcut.mFixScreenShortcutData_8702_2);
                            break;
                    }

                    if (fss != null && fss.pkgName != null && fss.screen != null && fss.screen_x != null && fss.screen_y != null) {
                        MMLog.d("FixScreenShortcut", "found default worspace: " + fss.pkgName + "," + fss.screen + "," + fss.screen_x + "," + fss.screen_y);
                        mFixScreenShortcut.addShortcut(context, fss.pkgName, fss.className, fss.screen, fss.screen_x, fss.screen_y);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
