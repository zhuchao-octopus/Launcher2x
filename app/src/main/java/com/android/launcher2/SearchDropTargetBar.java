/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.android.launcher.R;

/*
 * Ths bar will manage the transition between the QSB search bar and the delete drop
 * targets so that each of the individual IconDropTargets don't have to.
 */
public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {

    private static final int sTransitionInDuration = 200;
    private static final int sTransitionOutDuration = 175;

    private ObjectAnimator mDropTargetBarAnim;
    private ObjectAnimator mQSBSearchBarAnim;
    private static final AccelerateInterpolator sAccelerateInterpolator =
            new AccelerateInterpolator();

    private boolean mIsSearchBarHidden;
    private View mQSBSearchBar;
    private View mDropTargetBar;
    private ButtonDropTarget mInfoDropTarget;
    private ButtonDropTarget mDeleteDropTarget;
    private int mBarHeight;
    private boolean mDeferOnDragEnd = false;

    private Drawable mPreviousBackground;
    private boolean mEnableDropDownDropTargets;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup(Launcher launcher, DragController dragController) {
        dragController.addDragListener(this);
        dragController.addDragListener(mInfoDropTarget);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDropTarget(mInfoDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        dragController.setFlingToDeleteDropTarget(mDeleteDropTarget);
        mInfoDropTarget.setLauncher(launcher);
        mDeleteDropTarget.setLauncher(launcher);
    }

    private void prepareStartAnimation(View v) {
        // Enable the hw layers before the animation starts (will be disabled in the onAnimationEnd
        // callback below)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void setupAnimation(ObjectAnimator anim, final View v) {
        anim.setInterpolator(sAccelerateInterpolator);
        anim.setDuration(sTransitionInDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the individual components
        mQSBSearchBar = findViewById(R.id.qsb_search_bar);
        mDropTargetBar = findViewById(R.id.drag_target_bar);
        mInfoDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.info_target_text);
        mDeleteDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.delete_target_text);
        mBarHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);

        mInfoDropTarget.setSearchDropTargetBar(this);
        mDeleteDropTarget.setSearchDropTargetBar(this);

        mEnableDropDownDropTargets =
            getResources().getBoolean(R.bool.config_useDropTargetDownTransition);

        // Create the various fade animations
        if (mEnableDropDownDropTargets) {
            mDropTargetBar.setTranslationY(-mBarHeight);
            mDropTargetBarAnim = ObjectAnimator.ofFloat(mDropTargetBar, "translationY", -mBarHeight, 0f);
            mQSBSearchBarAnim = ObjectAnimator.ofFloat(mQSBSearchBar, "translationY", 0f, -mBarHeight);
        } else {
            mDropTargetBar.setAlpha(0f);
            mDropTargetBarAnim = ObjectAnimator.ofFloat(mDropTargetBar, "alpha", 0f, 1f);
            mQSBSearchBarAnim = ObjectAnimator.ofFloat(mQSBSearchBar, "alpha", 1f, 0f);
        }
        setupAnimation(mDropTargetBarAnim, mDropTargetBar);
        setupAnimation(mQSBSearchBarAnim, mQSBSearchBar);
    }

    public void finishAnimations() {
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.reverse();
        prepareStartAnimation(mQSBSearchBar);
        mQSBSearchBarAnim.reverse();
    }

    /*
     * Shows and hides the search bar.
     */
    public void showSearchBar(boolean animated) {
        if (!mIsSearchBarHidden) return;
        if (animated) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.reverse();
        } else {
            mQSBSearchBarAnim.cancel();
            if (mEnableDropDownDropTargets) {
                mQSBSearchBar.setTranslationY(0);
            } else {
                mQSBSearchBar.setAlpha(1f);
            }
        }
        mIsSearchBarHidden = false;
    }
    public void hideSearchBar(boolean animated) {
        if (mIsSearchBarHidden) return;
        if (animated) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.start();
        } else {
            mQSBSearchBarAnim.cancel();
            if (mEnableDropDownDropTargets) {
                mQSBSearchBar.setTranslationY(-mBarHeight);
            } else {
                mQSBSearchBar.setAlpha(0f);
            }
        }
        mIsSearchBarHidden = true;
    }

    /*
     * Gets various transition durations.
     */
    public int getTransitionInDuration() {
        return sTransitionInDuration;
    }
    public int getTransitionOutDuration() {
        return sTransitionOutDuration;
    }

    private boolean ifFullscreenOnDropTargetBar() {		//ww+
    	if (Utilities.mSystemUI != null &&
    		(Utilities.mSystemUI.equals(com.common.utils.MachineConfig.VALUE_SYSTEM_UI22_1050) ||
    		Utilities.mSystemUI.equals(com.common.utils.MachineConfig.VALUE_SYSTEM_UI31_KLD7) ||
    		Utilities.mSystemUI.equals(com.common.utils.MachineConfig.VALUE_SYSTEM_UI20_RM10_1) ||
    		Utilities.mSystemUI.equals(com.common.utils.MachineConfig.VALUE_SYSTEM_UI21_RM10_2)) &&
    		Launcher.mThis != null)
    		return true;
    	else
    		return false;
    }

    private boolean ifShowDeleteDragBar = false;
    private boolean isAllAppsApplication(DragSource source, Object info) {	//ww+,from DeleteDropTarget
        return (source instanceof AppsCustomizePagedView) && (info instanceof ApplicationInfo);
    }
    private boolean isAllAppsWidget(DragSource source, Object info) {
        if (source instanceof AppsCustomizePagedView) {
            if (info instanceof PendingAddItemInfo) {
                PendingAddItemInfo addInfo = (PendingAddItemInfo) info;
                switch (addInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        return true;
                }
            }
        }
        return false;
    }
    /*
     * DragController.DragListener implementation
     */
    @SuppressLint("SuspiciousIndentation")
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    	{	//ww+, if system app don't show DeleteDragBar
	    	ifShowDeleteDragBar = true;
	    	// If we are dragging a widget from AppsCustomize, hide the delete target
	        if (isAllAppsWidget(source, info)) {
	        	ifShowDeleteDragBar = false;
	        }
	        // If we are dragging an application from AppsCustomize, only show the control if we can
	        // delete the app (it was downloaded), and rename the string to "uninstall" in such a case
	        if (isAllAppsApplication(source, info)) {
	            ApplicationInfo appInfo = (ApplicationInfo) info;
	            if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) != 0) {
	            	ifShowDeleteDragBar = true;
	            } else {
	            	ifShowDeleteDragBar = false;
	            }
	            // If the user is not allowed to access the app details page or uninstall, then don't
	            // let them uninstall from here either.
	            UserManager userManager = (UserManager)
	                    getContext().getSystemService(Context.USER_SERVICE);
	            if (userManager.hasUserRestriction(UserManager.DISALLOW_APPS_CONTROL)
	                    || userManager.hasUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS)) {
	            	ifShowDeleteDragBar = false;
	            }
	        }
	    	if (!ifShowDeleteDragBar)
	    		return;
    	}

    	if (ifFullscreenOnDropTargetBar())	//ww+
        	Launcher.mThis.toggleFullScreen(true);

        // Animate out the QSB search bar, and animate in the drop target bar
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.start();
        if (!mIsSearchBarHidden) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.start();
        }
    }

    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
    	if (!ifShowDeleteDragBar)
    		return;
        if (!mDeferOnDragEnd) {
            // Restore the QSB search bar, and animate out the drop target bar
            prepareStartAnimation(mDropTargetBar);
            mDropTargetBarAnim.reverse();
            if (!mIsSearchBarHidden) {
                prepareStartAnimation(mQSBSearchBar);
                mQSBSearchBarAnim.reverse();
            }
        } else {
            mDeferOnDragEnd = false;
        }
        
        if (ifFullscreenOnDropTargetBar())	//ww+
			Launcher.mThis.toggleFullScreen(false);
    }

    public void onSearchPackagesChanged(boolean searchVisible, boolean voiceVisible) {
        if (mQSBSearchBar != null) {
            Drawable bg = mQSBSearchBar.getBackground();
            if (bg != null && (!searchVisible && !voiceVisible)) {
                // Save the background and disable it
                mPreviousBackground = bg;
                mQSBSearchBar.setBackgroundResource(0);
            } else if (mPreviousBackground != null && (searchVisible || voiceVisible)) {
                // Restore the background
                mQSBSearchBar.setBackground(mPreviousBackground);
            }
        }
    }

    public Rect getSearchBarBounds() {
        if (mQSBSearchBar != null) {
            final int[] pos = new int[2];
            mQSBSearchBar.getLocationOnScreen(pos);

            final Rect rect = new Rect();
            rect.left = pos[0];
            rect.top = pos[1];
            rect.right = pos[0] + mQSBSearchBar.getWidth();
            rect.bottom = pos[1] + mQSBSearchBar.getHeight();
            return rect;
        } else {
            return null;
        }
    }
}
