package com.android.launcher2;

import java.util.List;
import java.util.Locale;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.util.Log;

import com.android.launcher.R;
import com.android.launcher2.LauncherApplication;
import com.android.launcher2.LauncherSettings;
import com.android.launcher2.ShortcutInfo;

public class FixScreenShortcut{
	private static FixScreenShortcut mThis = null;	
	
	public static FixScreenShortcut getInstance() {
		if (mThis == null) {
			mThis = new FixScreenShortcut();
		}
		return mThis;
	}

	public static class FixScreenShortcutData {
		String pkgName;
		String className;
		String screen;
		String screen_x;
		String screen_y;
		
		FixScreenShortcutData(String pkgName, String className, String screen, String screen_x, String screen_y) {
			this.pkgName = pkgName;
			this.className = className;
			this.screen = screen;
			this.screen_x = screen_x;
			this.screen_y = screen_y;
		}
	}
	
	public static FixScreenShortcutData[] mFixScreenShortcutData_RM10_1 = new FixScreenShortcutData[] {
//			new FixScreenShortcutData("com.microntek.worship", "com.microntek.worship.WorshipActivity", "1", "0", "0"),
			new FixScreenShortcutData("com.ak.qibla", "com.ak.qibla.MainActivity", "1", "0", "0"),
			new FixScreenShortcutData("com.my.instructions", "com.my.instructions.InstructionsActivity", "1", "2", "1"),
			new FixScreenShortcutData("net.easyconn", "net.easyconn.ui.Sv05MainActivity", "1", "3", "1"),
			new FixScreenShortcutData("com.microntek.servicepoint", "com.microntek.servicepoint.ServicePointActivity", "2", "2", "0"),
			new FixScreenShortcutData("com.google.android.youtube", "com.google.android.apps.youtube.app.WatchWhileActivity", "2", "0", "1"),
			new FixScreenShortcutData("com.skype.raider", "com.skype.raider.Main", "2", "1", "1"),
			new FixScreenShortcutData("com.facebook.katana", "com.facebook.katana.LoginActivity", "2", "2", "1"),
			new FixScreenShortcutData("com.whatsapp", "com.whatsapp.Main", "2", "3", "1") };
	
	public static FixScreenShortcutData[] mFixScreenShortcutData_RM12 = new FixScreenShortcutData[] {
		new FixScreenShortcutData("com.ak.qibla", "com.ak.qibla.MainActivity", "1", "0", "0"),
		new FixScreenShortcutData("com.my.instructions", "com.my.instructions.InstructionsActivity", "1", "4", "1"),
		new FixScreenShortcutData("net.easyconn", "net.easyconn.ui.Sv05MainActivity", "1", "2", "1"), 
		new FixScreenShortcutData("com.google.android.youtube", "com.google.android.youtube.app.honeycomb.Shell$HomeActivity", "1", "4", "0")};
	
	public static FixScreenShortcutData[] mFixScreenShortcutData_8702_2 = new FixScreenShortcutData[] {
		new FixScreenShortcutData("com.zoulou.dab","com.zoulou.dab.activity.MainActivity", "0", "0", "-101"), 
	//	new FixScreenShortcutData("com.ex.dabplayer.pad", "com.ex.dabplayer.pad.activity.MainActivity", "0", "0", "-101"), 
//		new FixScreenShortcutData("com.estrongs.android.pop", "com.estrongs.android.pop.view.FileExplorerActivity", "1", "1", "1")
		};
	
	
	public static  FixScreenShortcutData[] mFixScreenShortcutData_RM10_2 = new FixScreenShortcutData[] {
//			new FixScreenShortcutData("com.microntek.worship", "com.microntek.worship.WorshipActivity", "3", "0", "1"),
			new FixScreenShortcutData("com.ak.qibla", "com.ak.qibla.MainActivity", "3", "0", "1"),
			new FixScreenShortcutData("com.my.instructions", "com.my.instructions.InstructionsActivity", "3", "3", "2")};

	public static  FixScreenShortcutData[] mFixScreenShortcutData_RM10_2_1280_480 = new FixScreenShortcutData[] {
//		new FixScreenShortcutData("com.microntek.worship", "com.microntek.worship.WorshipActivity", "1", "0", "0"),
		new FixScreenShortcutData("com.ak.qibla", "com.ak.qibla.MainActivity", "3", "3", "0"),
		new FixScreenShortcutData("com.my.instructions", "com.my.instructions.InstructionsActivity", "3", "1", "1"),
		new FixScreenShortcutData("net.easyconn", "net.easyconn.ui.Sv05MainActivity", "3", "2", "1"),
		new FixScreenShortcutData("com.microntek.servicepoint", "com.microntek.servicepoint.ServicePointActivity", "3", "4", "1"),
		new FixScreenShortcutData("com.google.android.youtube", "com.google.android.apps.youtube.app.WatchWhileActivity", "3", "5", "1"),
 };
	
	public static  FixScreenShortcutData[] mFixScreenShortcutData_RM10_1_1280_480 = new FixScreenShortcutData[] {
//		new FixScreenShortcutData("com.microntek.worship", "com.microntek.worship.WorshipActivity", "1", "0", "0"),
		new FixScreenShortcutData("com.ak.qibla", "com.ak.qibla.MainActivity", "1", "3", "0"),
		new FixScreenShortcutData("com.my.instructions", "com.my.instructions.InstructionsActivity", "1", "1", "1"),
		new FixScreenShortcutData("net.easyconn", "net.easyconn.ui.Sv05MainActivity", "1", "2", "1"),
		new FixScreenShortcutData("com.microntek.servicepoint", "com.microntek.servicepoint.ServicePointActivity", "1", "4", "1"),
		new FixScreenShortcutData("com.google.android.youtube", "com.google.android.apps.youtube.app.WatchWhileActivity", "1", "5", "1"),
 };
	
	public FixScreenShortcutData getFixScreenShortcutData(String pkgName, FixScreenShortcutData[] fixList) {
		for (int i = 0; fixList != null	&& i < fixList.length; i++) {
			if (fixList[i] != null
					&& fixList[i].pkgName != null
					&& fixList[i].pkgName.equals(pkgName)) {
				return fixList[i];
			}
		}
		return null;
	}
	
	private void removeShortcut(Context context, String pkgName) {
		if (context == null || pkgName == null) {
			Log.e("FixScreenShortcut", "removeShortcut param invalid");
		}
		try {
			ContentResolver cr = context.getContentResolver();
			int num = cr.delete(LauncherSettings.Favorites.CONTENT_URI, "intent like ? ", new String[]{"%" + pkgName + "%"});
			Log.d("FixScreenShortcut", "removeShortcut " + pkgName + " ok " + num);
		}catch(Exception e){
			Log.e("FixScreenShortcut", "removeShortcut " + pkgName + " failed");
		}
	}
	
	public boolean addShortcut(Context context, String pkgName,
			String className, String screen, String screen_x, String screen_y) {
		if ("-101".equals(screen_y)) {
			return addShortcutEx(context, pkgName, className, screen, screen_x,
					"0", LauncherSettings.Favorites.CONTAINER_HOTSEAT);
		} else {
			return addShortcutEx(context, pkgName, className, screen, screen_x,
					screen_y, LauncherSettings.Favorites.CONTAINER_DESKTOP);
		}
	}
	
	public boolean addShortcutEx(Context context, String pkgName, String className, String screen, String screen_x, String screen_y,
			int container) {
		String title = "unknown";
		String mainAct = null;
		int iconAppIdentifier = 0;
		int iconActivityIdentifier = 0;
		PackageManager pkgMag = context.getPackageManager();
		Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
		queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> list = pkgMag.queryIntentActivities(queryIntent, PackageManager.GET_ACTIVITIES);
		boolean fountPkg = false;
		for (int i = 0; i < list.size(); i++) {
			ResolveInfo info = list.get(i);
			if (info.activityInfo.packageName.equals(pkgName)) {
				fountPkg = true;
				title = info.loadLabel(pkgMag).toString();
				iconAppIdentifier = info.activityInfo.applicationInfo.icon;
				if (info.activityInfo.name != null && info.activityInfo.name.equals(className)) {
					mainAct = info.activityInfo.name;
					iconActivityIdentifier = info.activityInfo.icon;
					break;
				}
			}
		}
		if (!fountPkg)
			return false;
		if (mainAct == null) {
			Log.w("FixScreenShortcut", "can't find " + className + " in activityInfo");
			mainAct = className;
		}
		Log.d("FixScreenShortcut", "add shortcut " + pkgName + "," + mainAct + ", title:" + title + " to screen" + screen + " x=" + screen_x + " y=" + screen_y+"container="+container);		
		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
		shortcut.putExtra("duplicate", false);
//		shortcut.putExtra("screen", Integer.valueOf(screen));
//		shortcut.putExtra("cell_x", Integer.valueOf(screen_x));
//		shortcut.putExtra("cell_y", Integer.valueOf(screen_y));
		ComponentName comp = new ComponentName(pkgName, mainAct);
		removeShortcut(context, pkgName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setComponent(comp));
		Context pkgContext = null;
		if (context.getPackageName().equals(pkgName)) {
			pkgContext = context;
		} else {
			try {
				pkgContext = context.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		/*if (pkgContext != null) {
			ShortcutIconResource iconRes = null;
			if (iconActivityIdentifier != 0)
				iconRes = Intent.ShortcutIconResource.fromContext(pkgContext, iconActivityIdentifier);
			else if (iconAppIdentifier != 0)
				iconRes = Intent.ShortcutIconResource.fromContext(pkgContext, iconAppIdentifier);

			if (iconRes != null)
				shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
		}*/
//		context.sendBroadcast(shortcut);	//no use
		LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        ShortcutInfo info = app.getModel().addShortcutFix(context, shortcut,
        		container, Integer.valueOf(screen),
                Integer.valueOf(screen_x), Integer.valueOf(screen_y), true);
		return true;
	}
	
	public static CharSequence getAppAlternativeTitle(Context context, ComponentName cn, CharSequence title) {
		if (context == null || cn == null || title == null || Utilities.mSystemUI == null)
			return title;

		String pkgName = cn.getPackageName();
		String className = cn.getClassName();
		if (pkgName != null && className != null &&
			pkgName.equals("com.car.ui") &&
			className.endsWith(".FrontCameraActivity")) {
			if (Utilities.mSystemUI.equals(com.common.util.MachineConfig.VALUE_SYSTEM_UI28_7451))
				return context.getResources().getString(R.string.f_camera_alternative_7451);
			else if (Utilities.mSystemUI.equals(com.common.util.MachineConfig.VALUE_SYSTEM_UI22_1050))
				return context.getResources().getString(R.string.f_camera_alternative_1050);
		} else if ("com.eqset".equals(pkgName)) {
			if (Utilities.mIsDSP) {
				String locale = Locale.getDefault().getLanguage();
				Log.d("cde", ":"+locale);
				if (locale.equals("tr")) {
					return "Ses Ayarları";
				} else {
					return "DSP";
				}	
				
			}
		}else if (
				"com.google.android.maps.MapsActivity".equals(className)) {
			//for 619 custom
			String locale = Locale.getDefault().getLanguage();
			if (locale != null) {
				if (locale.equals("es")) {
					return "Mapas";
				} else if (locale.equals("ru")) {
					return "Карты";
				} 	
				
			}
			
			
			
		} else if (
				"net.easyconn.ui.Sv05MainActivity".equals(className)) {
			//for 1680 custom
			String locale = Locale.getDefault().getLanguage();
			if (locale != null) {
				if (locale.equals("tr")) {
					return "Kolay Bağlantı";
				} 		
				
			}
			
			
			
		} else if ("com.my.btmusic.BTMusicActivity".equals(className)) {

			if (com.common.util.MachineConfig.VALUE_SYSTEM_UI_9813.equals(Utilities.mSystemUI)) {
				String locale = Locale.getDefault().getLanguage();
				if (locale != null) {
					if (locale.equals("en")) {
						return "Music";
					} else if (locale.equals("pt")) {
						return "Musica";
					}

				}
			}

		} else if ("com.my.bt.ATBluetoothActivity".equals(className)) {

			if (com.common.util.MachineConfig.VALUE_SYSTEM_UI_9813.equals(Utilities.mSystemUI)) {
				String locale = Locale.getDefault().getLanguage();
				if (locale != null) {
					if (locale.equals("en")) {
						return "Phone";
					} else if (locale.equals("pt")) {
						return "Telefone";
					}

				}
			}

		}


		return title;
	}
}
