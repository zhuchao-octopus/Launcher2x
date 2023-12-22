package com.common.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

public class MarqueeTextView extends TextView {

	public MarqueeTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MarqueeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEllipsize(TextUtils.TruncateAt.MARQUEE);//
		setSingleLine(true);//
		setMarqueeRepeatLimit(-1);//
	}


	@Override
	public boolean isFocused() {
	    return true;
	}
}
