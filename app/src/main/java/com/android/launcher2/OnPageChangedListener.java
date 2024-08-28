package com.android.launcher2;

/**
 * 主屏页面滑动监听
 */
public interface OnPageChangedListener {
    /**
     * 页面滑动完毕
     *
     * @param page 当前选中页面索引
     */
    void onPageChanged(int page);
}
