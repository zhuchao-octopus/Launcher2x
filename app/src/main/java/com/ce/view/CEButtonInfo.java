package com.ce.view;

import android.content.ComponentName;

public class CEButtonInfo {
	public ComponentName mComponentName;
	public int mId;

	public CEButtonInfo(String s, int id) {
		try {
			String[] ss = s.split("/");
			String pack = ss[0];
			String name = ss[1];
			mComponentName = new ComponentName(pack, name);
			mId = id;
		} catch (Exception ignored) {
		}
	}
}
