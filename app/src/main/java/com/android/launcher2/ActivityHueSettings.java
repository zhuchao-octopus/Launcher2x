package com.android.launcher2;

import com.android.launcher.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.common.util.SystemConfig;

public class ActivityHueSettings extends Activity {
	private final static String TAG = "HueSettings";
	private final static String KEY_HUE = "key_hue";
	private SeekBar mSeekBar;
	private ImageView mIVNavi, mIVApps, mIVRadio, mIVLeftBG;
	private Button mBTNOk;

	private Bitmap mBitmap = null;
	private float mBmpWidth, mBmpHeight;
	private int mColor = -1;

	private void getDrawableInfo(Drawable drawable) {
		BitmapDrawable d = (BitmapDrawable) drawable;
		mBitmap = d.getBitmap();
		mBmpWidth = (float) mBitmap.getWidth();
		mBmpHeight = (float) mBitmap.getHeight();
		Log.d(TAG, "bmp width=" + mBmpWidth + " ,height=" + mBmpHeight);
		/*Toast.makeText(this,
				"bmp width=" + mBmpWidth + " ,height=" + mBmpHeight,
				Toast.LENGTH_LONG).show();*/
	}
	
	private int[] mInitHue = {-1, 0};	//[0] progress, [1]color
	public static boolean getSystemHue(Context c, int [] hue) {
		if (hue == null || hue.length != 2)
			return false;
		try {
			String str = SystemConfig.getProperty(c, KEY_HUE);
			if (str != null && !str.isEmpty()) {
				String[] orgHue = str.split(",");
				if (orgHue != null && orgHue.length == 2) {
					if (orgHue[0] != null && !orgHue[0].isEmpty())
						hue[0] = Integer.valueOf(orgHue[0]);
					if (orgHue[1] != null && !orgHue[1].isEmpty())
						hue[1] = Integer.valueOf(orgHue[1]);
					Log.d(TAG, "progress=" + hue[0] + " ,color=" + hue[1]);
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void finishLauncher() {
		if (Launcher.mThis != null) {
			Launcher.mThis.finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
		setContentView(R.layout.hue_settings);
		
		mSeekBar = (SeekBar) findViewById(R.id.seekhuebar);
		mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSeekBar.setMax(100);

		mIVLeftBG = (ImageView) findViewById(R.id.huepreimage);
		mIVNavi = (ImageView) findViewById(R.id.navi);
		mIVApps = (ImageView) findViewById(R.id.apps);
		mIVRadio = (ImageView) findViewById(R.id.radio);
		mBTNOk = (Button) findViewById(R.id.huebartext); 
		mBTNOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try {
					int progerss = mSeekBar.getProgress();
					if (mColor != -1 && mColor != mInitHue[1] && progerss != mInitHue[0]) {
						SystemConfig.setProperty(ActivityHueSettings.this, KEY_HUE, "" + progerss + "," + mColor);
						finish();
						finishLauncher();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		mBTNOk.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				SystemConfig.setProperty(ActivityHueSettings.this, KEY_HUE, "");
				finish();
				finishLauncher();
				return true;
			}
		});

		getDrawableInfo(mSeekBar.getProgressDrawable());
	}

	OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			float x = mBmpWidth * (progress / 100.0f);
			float y = mBmpHeight / 2;
			if (x >= mBmpWidth)
				x = mBmpWidth - 1;
			else if (x < 0)
				x = 0;

			if (y >= mBmpHeight)
				y = mBmpHeight - 1;
			else if (y < 0)
				y = 0;
			try {
				mColor = mBitmap.getPixel((int) x, (int) y);
	
				mIVLeftBG.setImageDrawable(tintDrawable(mIVLeftBG.getDrawable(), mColor));
				mIVNavi.setBackground(tintDrawable(mIVNavi.getBackground(), mColor));
				mIVApps.setBackground(tintDrawable(mIVApps.getBackground(), mColor));
				mIVRadio.setBackground(tintDrawable(mIVRadio.getBackground(), mColor));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	public static Drawable tintDrawable(Drawable drawable, int color) {
		Drawable wrappedDrawable = DrawableCompat.wrap(drawable).mutate();
		wrappedDrawable.setTintMode(android.graphics.PorterDuff.Mode.MULTIPLY);
		DrawableCompat.setTint(wrappedDrawable, color);
		return wrappedDrawable;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (getSystemHue(this, mInitHue) && mInitHue[0] != -1) {
			mSeekBar.setProgress(mInitHue[0]);
		}		
	}
}