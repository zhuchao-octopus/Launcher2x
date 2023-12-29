package com.common.presentation;

import android.widget.VideoView;
import android.util.Log;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter;
import android.app.Application;
import android.app.Presentation;
import android.os.Bundle;

import android.view.Display;
import android.content.Context;
import android.view.WindowManager;

public class PresentationManager {
	private static final String TAG = "PresentationManager";

	private Presentation mPresentation = null;

	private Context mContext;
	private Display[] mDisplay;

	private static PresentationManager mPresentationManager;

	public static PresentationManager getInstanse(Context context) {
		if (mPresentationManager == null) {
			mPresentationManager = new PresentationManager();
			mPresentationManager.mContext = context;
			mPresentationManager.init();
		}
		return mPresentationManager;
	}

	private void init() {
		DisplayManager displayManager = (DisplayManager) mContext
				.getSystemService(Context.DISPLAY_SERVICE);
		mDisplay = displayManager.getDisplays();
		Log.i(TAG, "get " + mDisplay.length + " displays!");
		// if (pd.length > 0) {
		// for (int i = 0; i < pd.length; i++) {
		// Display pdi = pd[i];
		// Log.i(TAG,
		// "get " + i + "display: " + pdi.getName() + " "
		// + pdi.getWidth() + "x" + pdi.getHeight());
		// }
		// if (pd.length > 1) {
		// mDisplay = pd[1];
		// }
		// }
	}

	public Display getDisplay(int index) {
		if (index < mDisplay.length) {
			return mDisplay[index];
		}
		return null;
	}

	public void updatePresentation(int index, Presentation presentation) {
		if (index < mDisplay.length) {
			Display display = mDisplay[index];

			// Dismiss the current presentation if the display has changed.
			if (mPresentation != null && presentation.getDisplay() != display) {
				Log.w(TAG,
						"Dismissing presentation because the current route no longer "
								+ "has a presentation display.");
				// if (mListener != null) {
				// mListener.onPresentationChanged(false);
				// }
				// mPresentation.dismiss();
				// mPresentation = null;
			}
			// Show a new presentation if needed.
			if (mPresentation == null && mDisplay != null) {
				Log.w(TAG, "Showing presentation on display: " + mDisplay);
				// mPresentation = new DemoPresentation(this,
				// presentationDisplay);
				try {
					WindowManager.LayoutParams l = mPresentation.getWindow()
							.getAttributes();
					// l.type = WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW +
					// 100;
					l.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
					mPresentation.show();

				} catch (WindowManager.InvalidDisplayException ex) {
					Log.w(TAG,
							"Couldn't show presentation!  Display was removed in "
									+ "the meantime.", ex);
					mPresentation = null;
				}

			}
		}
	}
}
