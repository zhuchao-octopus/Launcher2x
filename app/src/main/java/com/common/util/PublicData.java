package com.common.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.os.FileUtils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

public class PublicData {
	private final static String TAG = "PublicData";
	public final static String KEY_TOP_ACTIVITY = "key_top_activity";

	private final static String PROJECT_CONFIG = ".public_data";
	private final static String SYSTEM_CONFIG = MyCmd.VENDOR_DIR + PROJECT_CONFIG;

	private static Properties mPoperties = new Properties();

	// only CarService use these functions below. other use Settings
	private static void getConfigProperties() {
		InputStream inputStream = null;
		File configFile = new File(SYSTEM_CONFIG);
		if (configFile.exists()) {
			try {
				inputStream = new FileInputStream(configFile);
				mPoperties.load(inputStream);
				inputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void initConfigProperties() {
//		if (mPoperties.size() == 0) {
			getConfigProperties();
//		}
	}

	public static void updateConfigProperties() {
		Log.d(TAG,"updateConfigProperties()");
		File file = new File(SYSTEM_CONFIG);
		try {
			if (!file.exists()) {
				file.createNewFile();
				//FileUtils.setPermissions(SYSTEM_CONFIG, FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO, -1, -1);
			} else {

			}
			//FileUtils.setPermissions(SYSTEM_CONFIG, FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO, -1, -1);

			FileOutputStream out = new FileOutputStream(file);
			mPoperties.store(out, "");

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String getProperty(String name) {
		initConfigProperties();
		return mPoperties.getProperty(name);
	}

	public static Object setProperty(String name, String value) {
		Object ret;
		initConfigProperties();
		ret = mPoperties.setProperty(name, value);
		updateConfigProperties();
		return ret;
	}

}
