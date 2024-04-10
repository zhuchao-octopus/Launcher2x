package com.common.view;

import com.android.launcher.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;

public class MyScrollView extends HorizontalScrollView {
    private int lastScrollX;
    private int downScrollX = -1;
    private float downTouchX = -1;

    public MyScrollView(Context context) {
        this(context, null);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        // TODO Auto-generated method stub
        Log.d("ccc", "" + deltaX);
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // TODO Auto-generated method stub
        super.onScrollChanged(l, t, oldl, oldt);

        if (downScrollX == -1) {
            long time = System.currentTimeMillis() - mTimeTouch;
            Log.d("ddd", time + ":" + l);
            if (time > 800) {
                if (mWholdePage == -1) {
                    mWholdePage = PAGE[mDefaultPage];
                }

                handler.removeMessages(MSG_FOR_WHOLE_PAGE);
                handler.sendMessageDelayed(handler.obtainMessage(MSG_FOR_WHOLE_PAGE, l, 0), 220);
            }
        }
    }

    private int mWholdePage = -1;
    private long mTimeTouch;

    private void init(Context context) {
        Object tag = getTag();
        if (tag instanceof String) {
            String new_name = (String) tag;
            String[] ss = new_name.split(":");
            mDefaultPage = Integer.valueOf(ss[1]);
            ss = ss[0].split(",");
            if (ss != null && ss.length > 0) {
                PAGE = new int[ss.length];
                for (int i = 0; i < ss.length; ++i) {
                    try {
                        PAGE[i] = Integer.valueOf(ss[i]);
                    } catch (Exception e) {

                    }
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
        // TODO Auto-generated method stub

        super.onLayout(arg0, arg1, arg2, arg3, arg4);
        //		Log.d("aabb", "onLayout:");
        if (mDefaultPage != -1) {
            mPage = mDefaultPage;
            scrollTo(PAGE[mDefaultPage], 0);
            mDefaultPage = -1;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        if (mDefaultPage != -1) {
            // scrollTo(PAGE[mDefaultPage], 0);
            // mDefaultPage = -1;
        } else {
            checkInNewPage();
        }

        //		Log.d("aabb", "onDraw:");

        super.onDraw(canvas);
    }

    private final static int MSG_TOUCH = 0;
    private final static int MSG_CALLBACK = 1;

    private final static int MSG_FOR_WHOLE_PAGE = 3;

    private Handler handler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_TOUCH:
                    if (downScrollX == -1) {
                        return;
                    }
                    int scrollX = MyScrollView.this.getScrollX();

                    // 此时的距离和记录下的距离不相等，在隔5毫秒给handler发送消息

                    Log.d("ccddd", scrollX + ":" + lastScrollX + ":" + downScrollX);
                    mTimeTouch = System.currentTimeMillis();
                    if (lastScrollX != scrollX) {
                        lastScrollX = scrollX;
                        handler.sendEmptyMessageDelayed(MSG_TOUCH, 5);
                        // handler.sendMessageDelayed(handler.obtainMessage(), 5);
                    } else {

                        Log.d("ddddd", scrollX + "fffffffffffffff:" + downScrollX);

                        if (downScrollX < -1) {
                            doPage(downScrollX == -3, scrollX);
                        } else {
                            doPage(scrollX);
                        }

                        downScrollX = -1;
                    }
                    break;
                case MSG_FOR_WHOLE_PAGE:
                    doWholePage(msg.arg1);
                    break;
                case MSG_CALLBACK:
                    if (mICallBack != null) {
                        mICallBack.callback(msg.arg1, msg.arg2);
                    }
                    break;
            }

        }

        ;

    };

    OnGestureListener mOnGestureListener = new OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            // TODO Auto-generated method stub
            Log.d("eeee", e.getX() + ":");
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e2 != null) {
                float move = e2.getX() - downTouchX;

                Log.d("eeee", downTouchX + ":" + e2.getX() + ":" + move);

                if ((move > 150) || (move < -150)) {
                    if (move > 0) {
                        downScrollX = -2;
                    } else {
                        downScrollX = -3;
                    }
                    downTouchX = -1;
                    //					Log.d("eeee", downScrollX + "!!!!!!");
                }
            } else {
                //				Log.d("ddddd", e2 + ":" + e1 + ":onFling:" + velocityX);
            }
            // doNextPage(velocityX > 0);
            return false;
        }
    };
    GestureDetector mygesture = new GestureDetector(mOnGestureListener);

    private boolean mTouchDown = false;
    private boolean mUpThreePoint = false;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mTimeTouch = System.currentTimeMillis();

        if (!mUpThreePoint && ev.getPointerCount() >= 3) {
            mUpThreePoint = true;
        }

        if (mUpThreePoint && ev.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        if (downScrollX == -1) {
            downScrollX = getScrollX();
        }
        //		Log.d("aabb1", "ACTION_DOWN:"+ev.getAction());
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                lastScrollX = this.getScrollX();

                handler.sendEmptyMessageDelayed(MSG_TOUCH, 5);
                // handler.sendMessageDelayed(handler.obtainMessage(), 5);

                callBackMsg(CMD_TOUCH_EVENT, MotionEvent.ACTION_UP);
                mTouchDown = false;
                mUpThreePoint = false;
                break;

            case MotionEvent.ACTION_DOWN:
                mPreNewPage = -1;
                callBackMsg(CMD_TOUCH_EVENT, MotionEvent.ACTION_DOWN);
                mTouchDown = true;
                mUpThreePoint = false;
                Log.d("eeee", "ACTION_DOWN:" + ev.getX());
                downTouchX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                // mPreNewPage = -1;
                // Log.d("aabb", "ACTION_MOVE:"+ev.getPointerCount());
                callBackMsg(CMD_TOUCH_EVENT, MotionEvent.ACTION_MOVE);
                Log.d("eeee", "ACTION_MOVE:" + ev.getX());
                if (downTouchX == -1) {
                    downTouchX = ev.getX();
                }
                break;
        }
        mygesture.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private int mPage = 0;

    private int[] PAGE = {0, 1024};
    private int mDefaultPage = -1;


    void smoothScrollToEx(int x, int y) {
        Log.e("ddd", x + ":scroll:" + mPage);
        smoothScrollTo(x, y);
        int page = -1;
        for (int i = 0; i <= (PAGE.length - 1); ++i) {
            if (PAGE[i] == x) {
                page = i;
                break;
            }
        }
        if (page != -1) {
            callBackMsg(CMD_NEWPAGE, page);
        }
    }


    private int mType = 0;

    public void setPageType(int i) {
        mType = i;
    }

    private void doPage(int x) {
        int i = 0;
        if (mType == 0) {
            for (i = 0; i < (PAGE.length - 1); ++i) {
                if (x >= PAGE[i] && x <= (PAGE[i] + ((PAGE[i + 1] - PAGE[i]) / 2))) {
                    smoothScrollToEx(PAGE[i], 0);
                    mPage = i;
                    break;
                } else if (x <= PAGE[i + 1] && x > (PAGE[i] + ((PAGE[i + 1] - PAGE[i]) / 2))) {
                    smoothScrollToEx(PAGE[i + 1], 0);
                    mPage = i + 1;
                    break;
                }
            }
        } else if (mType == 1) {
            if (PAGE.length == 3) {
                if (x < PAGE[1] / 2) {
                    mPage = 0;
                } else if (x > (PAGE[1] + 160)) {
                    mPage = 2;
                } else {
                    mPage = 1;
                }
                smoothScrollToEx(PAGE[mPage], 0);
            }

        }
        //		Log.d("dddd", "22:"+mPage);
        showPageIndex(mPage);
    }

    //	@Override
    //	public void fling(int velocityX) {
    //		// TODO Auto-generated method stub
    //		super.fling(velocityX / 2);
    //	}

    private long mUserScrollTime = 0;

    public void scrollToPage(int page) {
        mUserScrollTime = System.currentTimeMillis();
        //		Log.d("ddddd", "scrollToPage:" + page);
        if (page >= 0 && page < PAGE.length) {
            mPage = page;
            smoothScrollToEx(PAGE[page], 0);
            callBackMsg(CMD_NEWPAGE, page);
            callBackMsg(CMD_PAGE_END, page);
        }
    }

    private void doPage(boolean b/*true is left flip*/, int x) {
        int i = 0;
        int page = -1;
        Log.d("ddddd", b + ":" + x + "11doPage:" + mPage);

        if (b) {
            if (mPage < (PAGE.length - 1)) {
                for (i = PAGE.length - 2; i >= 0; --i) {
                    if (x > PAGE[i]) {
                        smoothScrollToEx(PAGE[i + 1], 0);
                        page = i + 1;
                        break;
                    }
                }
            }
            //			else {
            //				return;
            //			}
        } else {
            if (mPage > 0) {
                for (i = 1; i <= (PAGE.length - 1); ++i) {
                    if (x <= PAGE[i]) {
                        smoothScrollToEx(PAGE[i - 1], 0);
                        page = i - 1;
                        break;
                    }
                }
            }
        }

        Log.d("ddddd", x + "22doPage:" + i);

        if (page != -1) {
            // mPage = i;
            mPage = page;
            showPageIndex(mPage);
        } else {
            doPage(x);
        }

        Log.d("ddddd", "33doPage:" + mPage);
    }

    private void showPageIndex(int index) {

        callBackMsg(CMD_PAGE_END, index);
        //		Log.d("dddd", "showPageIndex!!!!!!!!!!!!!:" + index);

        // for (int i = 0; i < 3; ++i) {
        //
        // View v = getRootView().findViewById(R.id.page1_ce+i);
        // if(v!=null){
        // try{
        // if(index==i){
        // v.getBackground().setLevel(1);
        // }else{
        // v.getBackground().setLevel(0);
        // }
        // }catch (Exception e){
        //
        // }
        // }
        // }
        mWholdePage = PAGE[index];
    }

    private int mPreNewPage = -1;

    private void checkInNewPage() {

        if (downScrollX < 0) {
            return;
        }

        boolean left = true;
        if ((getScrollX() - downScrollX) > 0) {
            left = false;
        }


        //		Log.d("aaa", left + ":checkInNewPage"+mPreNewPage);
        //		if (left){
        //			if (mPage == PAGE.length - 1)
        //			{
        //				return;
        //			}
        //		} else {
        //			if (mPage == 0){
        //				return;
        //			}
        //		}
        int newPage = -1;
        for (int i = 0; i < (PAGE.length - 1); ++i) {
            if (getScrollX() > PAGE[i] && getScrollX() < PAGE[i + 1]) {
                newPage = i;
                if (!left) {
                    newPage++;
                }
                break;
            }
        }

        Log.d("aaa", newPage + ":checkInNewPage" + mPreNewPage + ":" + getScrollX());
        if (newPage != -1 && mPreNewPage != newPage) {
            mPreNewPage = newPage;
            callBackMsg(CMD_NEWPAGE, newPage);
        }

    }

    private void callBackMsg(int cmd, int visible_page) {
        if (mICallBack != null) {
            handler.sendMessage(handler.obtainMessage(MSG_CALLBACK, cmd, visible_page));
        }
    }

    public static final int CMD_NEWPAGE = 0;
    public static final int CMD_TOUCH_EVENT = 1;
    public static final int CMD_PAGE_END = 2;

    public static interface ICallBack {
        public void callback(int cmd, int visible_page);
    }

    ;

    private ICallBack mICallBack;

    public void setCallback(ICallBack cb) {
        mICallBack = cb;
    }

    public int getCurPage() {
        return mPage;
    }

    public boolean getTouchDown() {
        return mTouchDown;
    }

    private void doWholePage(int x) {

        if (mUserScrollTime != 0 && (System.currentTimeMillis() - mUserScrollTime) < 2000) {
            Log.d("ccde", mWholdePage + ":time no doWholePage:" + x);
            mUserScrollTime = 0;
            //			return;
        }

        for (int i = 0; i <= (PAGE.length - 1); ++i) {
            if (x == PAGE[i]) {
                Log.d("ccde", mWholdePage + ":fix no doWholePage:" + x);
                mWholdePage = x;
                callBackMsg(CMD_NEWPAGE, i);
                return;
            }
        }

        Log.d("ccde", mWholdePage + ":" + x);
        int step = x - mWholdePage;
        if (step == 0) {
            Log.d("ccde", mWholdePage + ":time no doWholePage:" + x);
            return;
        }
        boolean b = (step > 0 ? true : false);
        doPage(b, x);
        mWholdePage = x;

        callBackMsg(CMD_NEWPAGE, mPage);
    }
}
