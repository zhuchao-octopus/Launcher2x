/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Advanceable;

import com.android.launcher.R;
import com.android.launcher2.InstallWidgetReceiver.WidgetMimeTypeHandlerData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.ce.view.WinceCEStyleApp;
import com.common.util.AppConfig;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.ProtocolAk47;
import com.common.util.SystemConfig;
import com.common.util.Util;
import com.zhuchao.android.fbase.MMLog;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherModel extends BroadcastReceiver {
    private static final String TAG = "LauncherModel";
    static final boolean DEBUG_LOADERS = false;

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons
    private final int mBatchSize; // 0 is all apps at once
    private final int mAllAppsLoadDelay; // milliseconds between batches

    private final boolean mAppsCanBeOnRemoveableStorage;

    private final LauncherApplication mApp;
    private final Object mLock = new Object();
    private final DeferredHandler mHandler = new DeferredHandler();
    private LoaderTask mLoaderTask;
    private boolean mIsLoaderTaskRunning;
    private volatile boolean mFlushingWorkerThread;

    // Specific runnable types that are run on the main thread deferred handler, this allows us to
    // clear all queued binding runnables when the Launcher activity is destroyed.
    private static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    private static final int MAIN_THREAD_BINDING_RUNNABLE = 1;


    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");

    static {
        sWorkerThread.start();
    }

    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());
    private static final Handler wHandler = new Handler();
    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    private boolean mWorkspaceLoaded;
    private boolean mAllAppsLoaded;

    // When we are loading pages synchronously, we can't just post the binding of items on the side
    // pages as this delays the rotation process.  Instead, we wait for a callback from the first
    // draw (in Workspace) to initiate the binding of the remaining side pages.  Any time we start
    // a normal load, we also clear this set of Runnables.
    static final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList<Runnable>();

    private WeakReference<Callbacks> mCallbacks;

    // < only access in worker thread >
    private final AllAppsList mBgAllAppsList;

    // The lock that must be acquired before referencing any static bg data structures.  Unlike
    // other locks, this one can generally be held long-term because we never expect any of these
    // static data structures to be referenced outside of the worker thread except on the first
    // load after configuration change.
    static final Object sBgLock = new Object();

    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    static final HashMap<Long, ItemInfo> sBgItemsIdMap = new HashMap<Long, ItemInfo>();

    // sBgWorkspaceItems is passed to bindItems, which expects a list of all folders and shortcuts
    //       created by LauncherModel that are directly on the home screen (however, no widgets or
    //       shortcuts within folders).
    static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();

    // sBgAppWidgets is all LauncherAppWidgetInfo created by LauncherModel. Passed to bindAppWidget()
    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets = new ArrayList<LauncherAppWidgetInfo>();

    // sBgFolders is all FolderInfos created by LauncherModel. Passed to bindFolders()
    static final HashMap<Long, FolderInfo> sBgFolders = new HashMap<Long, FolderInfo>();

    // sBgDbIconCache is the set of ItemInfos that need to have their icons updated in the database
    static final HashMap<Object, byte[]> sBgDbIconCache = new HashMap<Object, byte[]>();
    // </ only access in worker thread >

    private final IconCache mIconCache;
    private final Bitmap mDefaultIcon;

    private static int mCellCountX;
    private static int mCellCountY;

    protected int mPreviousConfigMcc;

    private final LauncherApps mLauncherApps;
    final UserManager mUserManager;
    private final LauncherApps.Callback mLauncherAppsCallback;

    public interface Callbacks {
        public boolean setLoadOnResume();

        public int getCurrentWorkspaceScreen();

        public void startBinding();

        public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end);

        public void bindFolders(HashMap<Long, FolderInfo> folders);

        public void finishBindingItems();

        public void bindAppWidget(LauncherAppWidgetInfo info);

        public void bindAllApplications(ArrayList<ApplicationInfo> apps);

        public void bindAppsAdded(ArrayList<ApplicationInfo> apps);

        public void bindAppsUpdated(ArrayList<ApplicationInfo> apps);

        public void bindComponentsRemoved(ArrayList<String> packageNames, ArrayList<ApplicationInfo> appInfos, boolean matchPackageNamesOnly, UserHandle user);

        public void bindPackagesUpdated(ArrayList<Object> widgetsAndShortcuts);

        public boolean isAllAppsVisible();

        public boolean isAllAppsButtonRank(int rank);

        public void bindSearchablesChanged();

        public void onPageBoundSynchronously(int page);
    }

    LauncherModel(LauncherApplication app, IconCache iconCache) {
        Log.d(TAG, "LauncherModel()");
        mAppsCanBeOnRemoveableStorage = Environment.isExternalStorageRemovable();
        mApp = app;
        mBgAllAppsList = new AllAppsList(iconCache);
        mIconCache = iconCache;

        mDefaultIcon = Utilities.createIconBitmap(mIconCache.getFullResDefaultActivityIcon(), app);

        final Resources res = app.getResources();
        mAllAppsLoadDelay = res.getInteger(R.integer.config_allAppsBatchLoadDelay);
        mBatchSize = res.getInteger(R.integer.config_allAppsBatchSize);
        Configuration config = res.getConfiguration();
        mPreviousConfigMcc = config.mcc;
        mLauncherApps = (LauncherApps) app.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        mUserManager = (UserManager) app.getSystemService(Context.USER_SERVICE);
        mLauncherAppsCallback = new LauncherAppsCallback();
    }

    /**
     * Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler.
     */
    private void runOnMainThread(Runnable r) {
        runOnMainThread(r, 0);
    }

    private void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /**
     * Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler.
     */
    private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    public Bitmap getFallbackIcon() {
        return Bitmap.createBitmap(mDefaultIcon);
    }

    public void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the " + "main thread");
        }

        // Clear any deferred bind runnables
        mDeferredBindRunnables.clear();
        // Remove any queued bind runnables
        mHandler.cancelAllRunnablesOfType(MAIN_THREAD_BINDING_RUNNABLE);
        // Unbind all the workspace items
        unbindWorkspaceItemsOnMainThread();
    }

    /**
     * Unbinds all the sBgWorkspaceItems and sBgAppWidgets on the main thread
     */
    void unbindWorkspaceItemsOnMainThread() {
        // Ensure that we don't use the same workspace items data structure on the main thread
        // by making a copy of workspace items first.
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ItemInfo item : tmpWorkspaceItems) {
                    item.unbind();
                }
                for (ItemInfo item : tmpAppWidgets) {
                    item.unbind();
                }
            }
        };
        runOnMainThread(r);
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    static void checkItemInfoLocked(final long itemId, final ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = sBgItemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (modelItem instanceof ShortcutInfo && item instanceof ShortcutInfo) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) && modelShortcut.intent.filterEquals(shortcut.intent) && modelShortcut.id == shortcut.id && modelShortcut.itemType == shortcut.itemType && modelShortcut.container == shortcut.container && modelShortcut.screen == shortcut.screen && modelShortcut.cellX == shortcut.cellX && modelShortcut.cellY == shortcut.cellY && modelShortcut.spanX == shortcut.spanX && modelShortcut.spanY == shortcut.spanY && ((modelShortcut.dropPos == null && shortcut.dropPos == null) || (modelShortcut.dropPos != null && shortcut.dropPos != null && modelShortcut.dropPos[0] == shortcut.dropPos[0] && modelShortcut.dropPos[1] == shortcut.dropPos[1]))) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") + "modelItem: " + ((modelItem != null) ? modelItem.toString() : "null") + "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
    }

    static void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        Runnable r = new Runnable() {
            public void run() {
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemInDatabaseHelper(Context context, final ContentValues values, final ItemInfo item, final String callingFunction) {
        final long itemId = item.id;
        final Uri uri = LauncherSettings.Favorites.getContentUri(itemId, false);
        final ContentResolver cr = context.getContentResolver();

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                cr.update(uri, values, null, null);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);

                    if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP && item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        // Item is in a folder, make sure this folder exists
                        if (!sBgFolders.containsKey(item.container)) {
                            // An items container is being set to a that of an item which is not in
                            // the list of Folders.
                            String msg = "item: " + item + " container being set to: " + item.container + ", not in the list of folders";
                            Log.e(TAG, msg);
                            Launcher.dumpDebugLogsToConsole();
                        }
                    }

                    // Items are added/removed from the corresponding FolderInfo elsewhere, such
                    // as in Workspace.onDrop. Here, we just add/remove them from the list of items
                    // that are on the desktop, as appropriate
                    ItemInfo modelItem = sBgItemsIdMap.get(itemId);
                    if (modelItem.container == LauncherSettings.Favorites.CONTAINER_DESKTOP || modelItem.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        switch (modelItem.itemType) {
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                if (!sBgWorkspaceItems.contains(modelItem)) {
                                    sBgWorkspaceItems.add(modelItem);
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        sBgWorkspaceItems.remove(modelItem);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    public void flushWorkerThread() {
        mFlushingWorkerThread = true;
        Runnable waiter = new Runnable() {
            public void run() {
                synchronized (this) {
                    notifyAll();
                    mFlushingWorkerThread = false;
                }
            }
        };

        synchronized (waiter) {
            runOnWorkerThread(waiter);
            if (mLoaderTask != null) {
                synchronized (mLoaderTask) {
                    mLoaderTask.notify();
                }
            }
            boolean success = false;
            while (!success) {
                try {
                    waiter.wait();
                    success = true;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    static void moveItemInDatabase(Context context, final ItemInfo item, final long container, final int screen, final int cellX, final int cellY) {
        String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ") --> " + "(" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 && container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        updateItemInDatabaseHelper(context, values, item, "moveItemInDatabase");
    }

    /**
     * Move and/or resize item in the DB to a new <container, screen, cellX, cellY, spanX, spanY>
     */
    static void modifyItemInDatabase(Context context, final ItemInfo item, final long container, final int screen, final int cellX, final int cellY, final int spanX, final int spanY) {
        String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ") --> " + "(" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 && container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SPANX, item.spanX);
        values.put(LauncherSettings.Favorites.SPANY, item.spanY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        updateItemInDatabaseHelper(context, values, item, "modifyItemInDatabase");
    }

    /**
     * Update an item to the database in a specified container.
     */
    static void updateItemInDatabase(Context context, final ItemInfo item) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(context, values);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        updateItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }

    /**
     * Returns true if the shortcuts already exists in the database.
     * we identify a shortcut by its title and intent.
     */
    static boolean shortcutExists(Context context, String title, Intent intent) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"title", "intent"}, "title=? and intent=?", new String[]{title, intent.toUri(0)}, null);
        boolean result = false;
        try {
            result = c.moveToFirst();
        } finally {
            c.close();
        }
        return result;
    }

    /**
     * Returns an ItemInfo array containing all the items in the LauncherModel.
     * The ItemInfo.id is not set through this function.
     */
    static ArrayList<ItemInfo> getItemsInLocalCoordinates(Context context) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.CONTAINER, LauncherSettings.Favorites.SCREEN, LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY, LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY, LauncherSettings.Favorites.PROFILE_ID}, null, null, null);

        final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
        final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
        final int profileIdIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
        final UserManager um = ((UserManager) context.getSystemService(Context.USER_SERVICE));
        try {
            while (c.moveToNext()) {
                ItemInfo item = new ItemInfo();
                item.cellX = c.getInt(cellXIndex);
                item.cellY = c.getInt(cellYIndex);
                item.spanX = c.getInt(spanXIndex);
                item.spanY = c.getInt(spanYIndex);
                item.container = c.getInt(containerIndex);
                item.itemType = c.getInt(itemTypeIndex);
                item.screen = c.getInt(screenIndex);
                int serialNumber = c.getInt(profileIdIndex);
                item.user = um.getUserForSerialNumber(serialNumber);
                // If the user no longer exists, skip this item
                if (item.user != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            items.clear();
        } finally {
            c.close();
        }

        return items;
    }

    /**
     * Find a folder in the db, creating the FolderInfo if necessary, and adding it to folderList.
     */
    FolderInfo getFolderById(Context context, HashMap<Long, FolderInfo> folderList, long id) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null, "_id=? and (itemType=? or itemType=?)", new String[]{String.valueOf(id), String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)}, null);

        try {
            if (c.moveToFirst()) {
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        folderInfo = findOrMakeFolder(folderList, id);
                        break;
                }

                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = c.getInt(containerIndex);
                folderInfo.screen = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);

                return folderInfo;
            }
        } finally {
            c.close();
        }

        return null;
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    static void addItemToDatabase(Context context, final ItemInfo item, final long container, final int screen, final int cellX, final int cellY, final boolean notify) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 && container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(context, values);

        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        item.id = app.getLauncherProvider().generateNewId();
        values.put(LauncherSettings.Favorites._ID, item.id);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Add item (" + item.title + ") to db, id: " + item.id + " (" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
                Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI : LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(item.id, item, stackTrace);
                    sBgItemsIdMap.put(item.id, item);
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.put(item.id, (FolderInfo) item);
                            // Fall through
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP || item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                sBgWorkspaceItems.add(item);
                            } else {
                                if (!sBgFolders.containsKey(item.container)) {
                                    // Adding an item to a folder that doesn't exist.
                                    String msg = "adding item: " + item + " to a folder that " + " doesn't exist";
                                    Log.e(TAG, msg);
                                    Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            break;
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Creates a new unique child id, for a given cell span across all layouts.
     */
    static int getCellLayoutChildId(long container, int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return (((int) container & 0xFF) << 24) | (screen & 0xFF) << 16 | (localCellX & 0xFF) << 8 | (localCellY & 0xFF);
    }

    static int getCellCountX() {
        return mCellCountX;
    }

    static int getCellCountY() {
        return mCellCountY;
    }

    /**
     * Updates the model orientation helper to take into account the current layout dimensions
     * when performing local/canonical coordinate transformations.
     */
    static void updateWorkspaceLayoutCells(int shortAxisCellCount, int longAxisCellCount) {
        mCellCountX = shortAxisCellCount;
        mCellCountY = longAxisCellCount;
    }

    /**
     * Removes the specified item from the database
     *
     * @param context
     * @param item
     */
    static void deleteItemFromDatabase(Context context, final ItemInfo item) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(item.id, false);

        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Delete item (" + item.title + ") from db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ")";
                Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.delete(uriToDelete, null, null);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.remove(item.id);
                            for (ItemInfo info : sBgItemsIdMap.values()) {
                                if (info.container == item.id) {
                                    // We are deleting a folder which still contains items that
                                    // think they are contained by that folder.
                                    String msg = "deleting a folder (" + item + ") which still " + "contains items (" + info + ")";
                                    Log.e(TAG, msg);
                                    Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.remove((LauncherAppWidgetInfo) item);
                            break;
                    }
                    sBgItemsIdMap.remove(item.id);
                    sBgDbIconCache.remove(item);
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Remove the contents of the specified folder from the database
     */
    static void deleteFolderContentsFromDatabase(Context context, final FolderInfo info) {
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    sBgItemsIdMap.remove(info.id);
                    sBgFolders.remove(info.id);
                    sBgDbIconCache.remove(info);
                    sBgWorkspaceItems.remove(info);
                }

                cr.delete(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        sBgItemsIdMap.remove(childInfo.id);
                        sBgDbIconCache.remove(childInfo);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    public LauncherApps.Callback getLauncherAppsCallback() {
        return mLauncherAppsCallback;
    }

    private class LauncherAppsCallback extends LauncherApps.Callback {
        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_UPDATE, new String[]{packageName}, user));
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REMOVE, new String[]{packageName}, user));
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, new String[]{packageName}, user));
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            if (!replacing) {
                enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, packageNames, user));
                if (mAppsCanBeOnRemoveableStorage) {
                    // Only rebind if we support removable storage. It catches the
                    // case where apps on the external sd card need to be reloaded.
                    startLoaderFromBackground();
                }
            } else {
                // If we are replacing then just update the packages in the list
                enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_UPDATE, packageNames, user));
            }
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            if (!replacing) {
                enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_UNAVAILABLE, packageNames, user));
            }
        }
    }

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    public void foreUpdateConfig(Context context) {
        Log.d(TAG, "crash let it restart finis:");
        //    	AppConfig.updateHideAppConfig();
        Launcher.finishLauncher();
        Launcher.updateUI();

        Utilities.mSystemUI = ResourceUtil.updateUi(context);
        Utilities.initStatics(context);
    }

    public void reloadWorkspace(Context context, final String name) {
        final LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        final LauncherProvider provider = app.getLauncherProvider();
        if (provider != null) {
            final int workspaceResId = !TextUtils.isEmpty(name) ? context.getResources().getIdentifier(name, "xml", "com.android.launcher") : 0;
            final boolean overridePrevious = true;
            MMLog.d(TAG, "workspace name: " + name + " id: " + workspaceResId);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    provider.loadDefaultFavoritesIfNecessary(workspaceResId, overridePrevious);
                    forceReload();
                }
            }).start();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MMLog.d(TAG, TAG + ".onReceive intent=" + intent);

        final String action = intent.getAction();
        switch (Objects.requireNonNull(action)) {
            case Intent.ACTION_LOCALE_CHANGED:
                // If we have changed locale we need to clear out the labels in all apps/workspace.
                foreUpdateConfig(context);
                //String s = null;
                //s.substring(0);
                forceReload();
                WinceCEStyleApp.forceReload();
                break;
            case Intent.ACTION_CONFIGURATION_CHANGED:
                // Check if configuration change was an mcc/mnc change which would affect app resources
                // and we would need to clear out the labels in all apps/workspace. Same handling as
                // above for ACTION_LOCALE_CHANGED
                foreUpdateConfig(context);
                Configuration currentConfig = context.getResources().getConfiguration();
                // if (mPreviousConfigMcc != currentConfig.mcc) {
                Log.d(TAG, "Reload apps on config change. curr_mcc:" + currentConfig.mcc + " prevmcc:" + mPreviousConfigMcc);
                forceReload();

                WinceCEStyleApp.forceReload();
                // }
                // Update previousConfig
                mPreviousConfigMcc = currentConfig.mcc;
                break;
            case SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED:
            case SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED:
                if (mCallbacks != null) {
                    Callbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindSearchablesChanged();
                    }
                }
                break;
            case MyCmd.BROADCAST_MACHINECONFIG_UPDATE:
                String s = intent.getStringExtra(MyCmd.EXTRA_COMMON_CMD);
                Log.d(TAG, "BROADCAST_MACHINECONFIG_UPDATE!!!!!!=" + s);

                if (MachineConfig.KEY_APP_HIDE.equals(s) || MachineConfig.KEY_CAN_BOX.equals(s)) {

                    int dsp = SystemConfig.getIntProperty(context, SystemConfig.KEY_DSP);
                    Utilities.mIsDSP = (dsp == 1);
                    Log.d("allen", "ResourceUtil Utilities.mIsDSP=" + Utilities.mIsDSP);

                    AppConfig.updateHideAppConfig();
                    AppConfig.addCustomHideApp(context);
                    if (MachineConfig.KEY_APP_HIDE.equals(s)) {    //ww+
                        if (ResourceUtil.ifLoadDvdHideWorkspace()) reloadWorkspace(context, "default_workspace_dvdhide");
                        else {
                            if (Launcher.ICON_TYPE_DEFAULT_WORKSPACE1 == Launcher.mIconType) {
                                reloadWorkspace(context, "default_workspace1");
                            } else {
                                if (MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(Utilities.mSystemUI) && AppConfig.isUSBDvd()) {
                                    reloadWorkspace(context, "default_workspace_usbdvd");
                                } else {
                                    reloadWorkspace(context, "default_workspace");
                                }
                            }
                        }
                    } else {
                        forceReload();
                    }
                    WinceCEStyleApp.forceReload();
                    //Log.d(TAG, "BROADCAST_MACHINECONFIG_UPDATE2222222!!!!!!="+s);
                    //resetLoadedState(false, true);
                    //startLoaderFromBackground();
                } else if (MachineConfig.KEY_SAVE_DRIVER.equals(s)) {

                    Launcher.updateSaveDriveMenuPublic();
                } else if (SystemConfig.KEY_LAUNCHER_UI_RM10.equals(s)) {
                    foreUpdateConfig(context);
                    //forceReload();
                    if (ResourceUtil.ifLoadDvdHideWorkspace()) reloadWorkspace(context, "default_workspace_dvdhide");
                    else {
                        if (MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(Utilities.mSystemUI) && AppConfig.isUSBDvd()) {
                            reloadWorkspace(context, "default_workspace_usbdvd");
                        } else {
                            reloadWorkspace(context, "default_workspace");
                        }
                        // reloadWorkspace(context, "default_workspace");
                    }
                    SystemConfig.setIntProperty(context, SystemConfig.KEY_LAUNCHER_UI_RM10_WORKSPACE_RELOAD, 0);
                    Log.d(TAG, "reload default workspace on machine config update");
                } else if (SystemConfig.KEY_DARK_MODE_SWITCH.equals(s)) {
                    Utilities.mDarkSwitch = SystemConfig.getIntProperty(context, SystemConfig.KEY_DARK_MODE_SWITCH);
                    if (Utilities.mDarkSwitch == 1) {
                        if (Utilities.mDark) {
                            mDark = true;
                            updateDarkMode(context);

                            if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
                                RadioMusicWidgetView.doDarkMode(true);
                            }
                        }
                    } else {
                        if (Utilities.mDark) {
                            mDark = false;
                            updateDarkMode(context);

                            if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
                                RadioMusicWidgetView.doDarkMode(false);
                            }
                        }
                    }
                } else if (MachineConfig.KEY_TOUCH3_IDENTIFY.equals(s)) {
                    Launcher.getTouch3ConfigValue();
                }
                break;
            case MyCmd.BROADCAST_ILL_STATUS:
                Utilities.mDark = intent.getBooleanExtra(MyCmd.EXTRA_COMMON_DATA, false);
                if (Utilities.mDarkSwitch == 1) {
                    mDark = Utilities.mDark;
                    updateDarkMode(context);

                    if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
                        RadioMusicWidgetView.doDarkMode(mDark);
                    }
                }
                break;
        }
    }

    private boolean mDark = false;
    private boolean mPreDark = false;

    @SuppressLint("HandlerLeak")
    private Handler mDarkHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                setWallpaperSync();
            }
        }
    };

    private void setWallpaperSync() {
        Log.d(TAG, "setWallpaperSync:" + mLoader);
        if (mLoader == null) {
            mLoader = (WallpaperLoader) new WallpaperLoader().execute();
        } else {
            mDarkHandler.sendEmptyMessageDelayed(0, 400);
        }
    }

    private void doDarkMode() {
        //		mIconCache.flush();
        //		forceReload();
        mDarkHandler.removeMessages(0);
        setWallpaperSync();
    }

    private void updateDarkMode(Context context) {
        if (MachineConfig.VALUE_SYSTEM_UI40_KLD90.equals(Utilities.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_9813.equals(Utilities.mSystemUI)) {

            int delay = 0;
            if (Launcher.mThis != null) {
                if (Launcher.mThis.isAllAppsVisible()) {
                    Launcher.mThis.showWorkspace(false);
                    delay = 200;
                } else {
                    delay = 1;
                }
            }

            if (mDarkHandler != null) {
                mDarkHandler.removeMessages(0);
            }

            if (delay != 0) {
                //				if (mDarkHandler == null) {
                //					mDarkHandler = new Handler() {
                //						@Override
                //						public void handleMessage(Message msg) {
                //							switch (msg.what) {
                //							case 0:
                //								doDarkMode();
                //								break;
                //							}
                //						}
                //					};
                //				}
                mDarkHandler.sendEmptyMessageDelayed(0, delay);
            } else {
                doDarkMode();
            }


        }
    }


    private boolean isEquals(Bitmap b1, Bitmap b2) {
        if (b1 == null || b2 == null) {
            return false;
        }
        // 先判断宽高是否一致，不一致直接返回false
        if (b1.getWidth() == b2.getWidth() && b1.getHeight() == b2.getHeight()) {
            int xCount = b1.getWidth();
            int yCount = b1.getHeight();
            for (int x = 0; x < xCount; x++) {
                for (int y = 0; y < yCount; y++) {
                    // 比较每个像素点颜色
                    if (b1.getPixel(x, y) != b2.getPixel(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @SuppressLint("ResourceType")
    private void updateWallpaperAsync(Context context) {

        if (mPreDark == mDark) {
            return;
        }
        mPreDark = mDark;
        String path;
        Bitmap bmp = null;
        WallpaperManager wpm = WallpaperManager.getInstance(context);

        WallpaperInfo wi = wpm.getWallpaperInfo();
        if (wi != null) {
            return;
        }

        try {
            //Log.d("ff", "4 set:"+mDark);
            if (!mDark) {
                path = SystemConfig.PATH_CE_WALLPAPER + SystemConfig.PATH_DARK_MODE_WALLPAPER;

                File f = new File(path);
                if (!f.exists()) {
                    path = SystemConfig.PATH_WALLPAPER + SystemConfig.PATH_DEFAULT_WALLPAPER0;
                }
                bmp = BitmapFactory.decodeFile(path);

                if (bmp != null) {
                    Log.d(TAG, "wpm.setBitmap:" + path);
                    wpm.setBitmap(bmp);
                }
            } else {
                Drawable old = wpm.getDrawable();
                Drawable darkDrawable = context.getResources().getDrawable(R.drawable.dark_background, null);
                saveOldWallpaper(context, old, darkDrawable);
                wpm.setResource(R.drawable.dark_background);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception:" + e);
        }

    }

    private WallpaperLoader mLoader;

    class WallpaperLoader extends AsyncTask<Void, Integer, Integer> {
        WallpaperLoader() {
            Log.d(TAG, "WallpaperLoader create");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isCancelled()) return 0;

            mIconCache.flush();
            forceReload();

            updateWallpaperAsync(Launcher.mThis);
            mLoader = null;

            Log.d("eee", "WallpaperLoader finish:");
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    private Drawable mOldWallpaper; // = wpm.getDrawable();

    private void saveOldWallpaper(Context context, Drawable old, Drawable dark) {

        // Log.d("ff", "saveOldWallpaper 1111111");
        // WallpaperManager wpm = WallpaperManager.getInstance(context);

        Bitmap bmpNow = null;
        Bitmap bmpOld = null;
        if (old != null) {
            if (old instanceof BitmapDrawable) {
                BitmapDrawable new_name = (BitmapDrawable) old;
                bmpNow = new_name.getBitmap();
            }
        }

        if (dark != null) {
            if (dark instanceof BitmapDrawable) {
                BitmapDrawable new_name = (BitmapDrawable) dark;
                Bitmap bmpDark = new_name.getBitmap();

                boolean equal = isEquals(bmpNow, bmpDark);
                if (equal) {
                    Log.d("eee", "saveOldWallpaper same:");
                    return;
                }
            }
        }

        String path;


        path = SystemConfig.PATH_CE_WALLPAPER + SystemConfig.PATH_DARK_MODE_WALLPAPER;

        File f = new File(path);
        if (f.exists()) {
            bmpOld = BitmapFactory.decodeFile(path);
        }

        boolean equal = isEquals(bmpNow, bmpOld);

        if (!equal) {
            checkDir();
            saveBmpFile(bmpNow, path);

            mOldWallpaper = old;
        }
    }

    private void checkDir() {
        try {
            File f = (new File(SystemConfig.PATH_CE_WALLPAPER));
            if (!f.exists()) {
                f.mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "checkDir:", e);
        }
    }

    private void saveBmpFile(Bitmap img, String path) {
        try {
            OutputStream os = new FileOutputStream(path);
            img.compress(CompressFormat.PNG, 100, os);
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "save:", e);
        }
    }

    void forceReload() {
        resetLoadedState(true, true);

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    /**
     * When the launcher is in the background, it's possible for it to miss paired
     * configuration changes.  So whenever we trigger the loader from the background
     * tell the launcher that it needs to re-run the loader when it comes back instead
     * of doing it now.
     */
    public void startLoaderFromBackground() {
        boolean runLoader = false;
        if (mCallbacks != null) {
            Callbacks callbacks = mCallbacks.get();
            if (callbacks != null) {
                // Only actually run the loader if they're not paused.
                if (!callbacks.setLoadOnResume()) {
                    runLoader = true;
                }
            }
        }
        if (runLoader) {
            startLoader(false, -1);
        }
    }

    // If there is already a loader task running, tell it to stop.
    // returns true if isLaunching() was true on the old task
    private boolean stopLoaderLocked() {
        boolean isLaunching = false;
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        return isLaunching;
    }

    public void startLoader(boolean isLaunching, int synchronousBindPage) {
        synchronized (mLock) {
            ///if (DEBUG_LOADERS) {
            MMLog.d(TAG, "startLoader isLaunching=" + isLaunching);
            ///}

            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            mDeferredBindRunnables.clear();

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                // also, don't downgrade isLaunching if we're already running
                isLaunching = isLaunching || stopLoaderLocked();
                mLoaderTask = new LoaderTask(mApp, isLaunching);
                if (synchronousBindPage > -1 && mAllAppsLoaded && mWorkspaceLoaded) {
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);
                }
            }
        }
    }

    void bindRemainingSynchronousPages() {
        // Post the remaining side pages to be loaded
        if (!mDeferredBindRunnables.isEmpty()) {
            for (final Runnable r : mDeferredBindRunnables) {
                mHandler.post(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
            mDeferredBindRunnables.clear();
        }
    }

    public void stopLoader() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                mLoaderTask.stopLocked();
            }
        }
    }

    public boolean isAllAppsLoaded() {
        return mAllAppsLoaded;
    }

    boolean isLoadingWorkspace() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                return mLoaderTask.isLoadingWorkspace();
            }
        }
        return false;
    }

    /**
     * Runnable for the thread that loads the contents of the launcher:
     * - workspace icons
     * - widgets
     * - all apps icons
     */
    private class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        private boolean mIsLoadingAndBindingWorkspace;
        private boolean mStopped;
        private boolean mLoadAndBindStepFinished;

        private HashMap<Object, CharSequence> mLabelCache;

        LoaderTask(Context context, boolean isLaunching) {
            mContext = context;
            mIsLaunching = isLaunching;
            mLabelCache = new HashMap<Object, CharSequence>();
        }

        boolean isLaunching() {
            return mIsLaunching;
        }

        boolean isLoadingWorkspace() {
            return mIsLoadingAndBindingWorkspace;
        }

        private void loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;

            // Load the workspace
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindWorkspace mWorkspaceLoaded=" + mWorkspaceLoaded);
            }

            if (!mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }

            // Bind the workspace
            bindWorkspace(-1);
        }

        private void waitForIdle() {
            // Wait until the either we're stopped or the other threads are done.
            // This way we don't start loading all apps until the workspace has settled
            // down.
            synchronized (LoaderTask.this) {
                final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                mHandler.postIdle(new Runnable() {
                    public void run() {
                        synchronized (LoaderTask.this) {
                            mLoadAndBindStepFinished = true;
                            if (DEBUG_LOADERS) {
                                Log.d(TAG, "done with previous binding step");
                            }
                            LoaderTask.this.notify();
                        }
                    }
                });

                while (!mStopped && !mLoadAndBindStepFinished && !mFlushingWorkerThread) {
                    try {
                        // Just in case mFlushingWorkerThread changes but we aren't woken up,
                        // wait no longer than 1sec at a time
                        this.wait(1000);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "waited " + (SystemClock.uptimeMillis() - workspaceWaitTime) + "ms for previous step to finish binding");
                }
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage < 0) {
                // Ensure that we have a valid page index to load synchronously
                throw new RuntimeException("Should not call runBindSynchronousPage() without " + "valid page index");
            }
            if (!mAllAppsLoaded || !mWorkspaceLoaded) {
                // Ensure that we don't try and bind a specified page when the pages have not been
                // loaded already (we should load everything asynchronously in that case)
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (mLock) {
                if (mIsLoaderTaskRunning) {
                    // Ensure that we are never running the background loading at this point since
                    // we also touch the background collections
                    throw new RuntimeException("Error! Background loading is already running");
                }
            }

            // XXX: Throw an exception if we are already loading (since we touch the worker thread
            //      data structures, we can't allow any other thread to touch that data, but because
            //      this call is synchronous, we can get away with not locking).

            // The LauncherModel is static in the LauncherApplication and mHandler may have queued
            // operations from the previous activity.  We need to ensure that all queued operations
            // are executed before any synchronous binding work is done.
            mHandler.flush();

            // Divide the set of loaded items into those that we are binding synchronously, and
            // everything else that is to be bound normally (asynchronously).
            bindWorkspace(synchronousBindPage);
            // XXX: For now, continue posting the binding of AllApps as there are other issues that
            //      arise from that.
            onlyBindAllApps();
        }

        public void run() {
            synchronized (mLock) {
                mIsLoaderTaskRunning = true;
            }

            keep_running:
            {
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to " + (mIsLaunching ? "DEFAULT" : "BACKGROUND"));
                    Process.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }

                // First step. Load workspace first, this is necessary since adding of apps from
                // managed profile in all apps is deferred until onResume. See http://b/17336902.
                if (DEBUG_LOADERS) Log.d(TAG, "step 1: loading workspace");
                loadAndBindWorkspace();

                if (mStopped) {
                    break keep_running;
                }

                // Whew! Hard work done.  Slow us down, and wait until the UI thread has
                // settled down.
                synchronized (mLock) {
                    if (mIsLaunching) {
                        if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to BACKGROUND");
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                waitForIdle();

                // Second step. Load all apps.
                if (DEBUG_LOADERS) Log.d(TAG, "step 2: loading all apps");
                loadAndBindAllApps();

                // Restore the default thread priority after we are done loading items
                synchronized (mLock) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }


            // Update the saved icons if necessary
            if (DEBUG_LOADERS) Log.d(TAG, "Comparing loaded icons to database icons");
            synchronized (sBgLock) {
                for (Object key : sBgDbIconCache.keySet()) {
                    updateSavedIcon(mContext, (ShortcutInfo) key, sBgDbIconCache.get(key));
                }
                sBgDbIconCache.clear();
            }

            // Clear out this reference, otherwise we end up holding it until all of the
            // callback runnables are done.
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                mIsLoaderTaskRunning = false;
            }
        }

        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
            }
        }

        /**
         * Gets the callbacks object.  If we've been stopped, or if the launcher object
         * has somehow been garbage collected, return null instead.  Pass in the Callbacks
         * object that was around when the deferred message was scheduled, and if there's
         * a new Callbacks object around then also return null.  This will save us from
         * calling onto it with data that will be ignored.
         */
        Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
            synchronized (mLock) {
                if (mStopped) {
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final Callbacks callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }

        // check & update map of what's occupied; used to discard overlapping/invalid items
        private boolean checkItemPlacement(ItemInfo occupied[][][], ItemInfo item) {
            int containerIndex = item.screen;
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                // Return early if we detect that an item is under the hotseat button
                if (mCallbacks == null || mCallbacks.get().isAllAppsButtonRank(item.screen)) {
                    return false;
                }

                // We use the last index to refer to the hotseat and the screen as the rank, so
                // test and update the occupied state accordingly
                if (occupied[Launcher.SCREEN_COUNT][item.screen][0] != null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY + ") occupied by " + occupied[Launcher.SCREEN_COUNT][item.screen][0]);
                    return false;
                } else {
                    occupied[Launcher.SCREEN_COUNT][item.screen][0] = item;
                    return true;
                }
            } else if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                // Skip further checking if it is not the hotseat or workspace container
                return true;
            }

            // Check if any workspace icons overlap with each other
            for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                    if (occupied[containerIndex][x][y] != null) {
                        Log.e(TAG, "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screen + ":" + x + "," + y + ") occupied by " + occupied[containerIndex][x][y]);
                        return false;
                    }
                }
            }
            for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                    occupied[containerIndex][x][y] = item;
                }
            }

            return true;
        }

        private void loadWorkspace() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();
            boolean overridePrevious = true;
            // Make sure the default workspace is loaded, if needed
            mApp.getLauncherProvider().loadDefaultFavoritesIfNecessary(0, overridePrevious);

            synchronized (sBgLock) {
                sBgWorkspaceItems.clear();
                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                sBgDbIconCache.clear();

                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();

                final Cursor c = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);

                // +1 for the hotseat (it can be larger than the workspace)
                // Load workspace in reverse order to ensure that latest items are loaded first (and
                // before any earlier duplicates)
                final ItemInfo occupied[][][] = new ItemInfo[Launcher.SCREEN_COUNT + 1][mCellCountX + 1][mCellCountY + 1];

                try {
                    final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
                    final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                    final int iconTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    final int iconPackageIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
                    final int iconResourceIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
                    final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                    final int appWidgetIdIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                    final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                    final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                    final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                    final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                    final int profileIdIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
                    //final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                    //final int displayModeIndex = c.getColumnIndexOrThrow(
                    //        LauncherSettings.Favorites.DISPLAY_MODE);

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    int container;
                    long id;
                    Intent intent;
                    UserHandle user;

                    while (!mStopped && c.moveToNext()) {
                        try {
                            int itemType = c.getInt(itemTypeIndex);

                            switch (itemType) {
                                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                    intentDescription = c.getString(intentIndex);
                                    int serialNumber = c.getInt(profileIdIndex);
                                    user = mUserManager.getUserForSerialNumber(serialNumber);
                                    // If the user doesn't exist anymore, skip.
                                    if (user == null) {
                                        itemsToRemove.add(c.getLong(idIndex));
                                        continue;
                                    }
                                    try {
                                        intent = Intent.parseUri(intentDescription, 0);
                                    } catch (URISyntaxException e) {
                                        continue;
                                    }

                                    if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                        info = getShortcutInfo(manager, intent, user, context, c, iconIndex, titleIndex, mLabelCache);
                                    } else {
                                        info = getShortcutInfo(c, context, iconTypeIndex, iconPackageIndex, iconResourceIndex, iconIndex, titleIndex);

                                        // App shortcuts that used to be automatically added to Launcher
                                        // didn't always have the correct intent flags set, so do that
                                        // here
                                        if (intent.getAction() != null && intent.getCategories() != null && intent.getAction().equals(Intent.ACTION_MAIN) && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                        }
                                    }


                                    if (info != null) {
                                        info.intent = intent;
                                        info.id = c.getLong(idIndex);
                                        container = c.getInt(containerIndex);
                                        info.container = container;
                                        info.screen = c.getInt(screenIndex);
                                        info.cellX = c.getInt(cellXIndex);
                                        info.cellY = c.getInt(cellYIndex);
                                        info.intent.putExtra(ItemInfo.EXTRA_PROFILE, info.user);

                                        // check & update map of what's occupied
                                        if (!checkItemPlacement(occupied, info)) {
                                            break;
                                        }

                                        switch (container) {
                                            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                                sBgWorkspaceItems.add(info);
                                                break;
                                            default:
                                                // Item is in a user folder
                                                FolderInfo folderInfo = findOrMakeFolder(sBgFolders, container);
                                                folderInfo.add(info);
                                                break;
                                        }
                                        sBgItemsIdMap.put(info.id, info);

                                        // now that we've loaded everthing re-save it with the
                                        // icon in case it disappears somehow.
                                        queueIconToBeChecked(sBgDbIconCache, info, c, iconIndex);
                                    } else {
                                        // Failed to load the shortcut, probably because the
                                        // activity manager couldn't resolve it (maybe the app
                                        // was uninstalled), or the db row was somehow screwed up.
                                        // Delete it.
                                        id = c.getLong(idIndex);
                                        Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                                        contentResolver.delete(LauncherSettings.Favorites.getContentUri(id, false), null, null);
                                    }
                                    break;

                                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                    id = c.getLong(idIndex);
                                    FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                    folderInfo.title = c.getString(titleIndex);
                                    folderInfo.id = id;
                                    container = c.getInt(containerIndex);
                                    folderInfo.container = container;
                                    folderInfo.screen = c.getInt(screenIndex);
                                    folderInfo.cellX = c.getInt(cellXIndex);
                                    folderInfo.cellY = c.getInt(cellYIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, folderInfo)) {
                                        break;
                                    }
                                    switch (container) {
                                        case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                        case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                            sBgWorkspaceItems.add(folderInfo);
                                            break;
                                    }

                                    sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                    sBgFolders.put(folderInfo.id, folderInfo);
                                    break;

                                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                    // Read all Launcher-specific widget details
                                    int appWidgetId = c.getInt(appWidgetIdIndex);
                                    id = c.getLong(idIndex);

                                    final AppWidgetProviderInfo provider = widgets.getAppWidgetInfo(appWidgetId);

                                    if (!isSafeMode && (provider == null || provider.provider == null || provider.provider.getPackageName() == null)) {
                                        String log = "Deleting widget that isn't installed anymore: id=" + id + " appWidgetId=" + appWidgetId;
                                        Log.e(TAG, log);
                                        Launcher.sDumpLogs.add(log);
                                        itemsToRemove.add(id);
                                    } else {
                                        appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, provider.provider);
                                        appWidgetInfo.id = id;
                                        appWidgetInfo.screen = c.getInt(screenIndex);
                                        appWidgetInfo.cellX = c.getInt(cellXIndex);
                                        appWidgetInfo.cellY = c.getInt(cellYIndex);
                                        appWidgetInfo.spanX = c.getInt(spanXIndex);
                                        appWidgetInfo.spanY = c.getInt(spanYIndex);
                                        int[] minSpan = Launcher.getMinSpanForWidget(context, provider);
                                        appWidgetInfo.minSpanX = minSpan[0];
                                        appWidgetInfo.minSpanY = minSpan[1];

                                        container = c.getInt(containerIndex);
                                        if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP && container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                            Log.e(TAG, "Widget found where container != " + "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                            continue;
                                        }
                                        appWidgetInfo.container = c.getInt(containerIndex);

                                        // check & update map of what's occupied
                                        if (!checkItemPlacement(occupied, appWidgetInfo)) {
                                            break;
                                        }
                                        sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                        sBgAppWidgets.add(appWidgetInfo);
                                    }
                                    break;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Desktop items loading interrupted:", e);
                        }
                    }
                } finally {
                    c.close();
                }

                if (itemsToRemove.size() > 0) {
                    ContentProviderClient client = contentResolver.acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI);
                    // Remove dead items
                    for (long id : itemsToRemove) {
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "Removed id = " + id);
                        }
                        // Don't notify content observers
                        try {
                            client.delete(LauncherSettings.Favorites.getContentUri(id, false), null, null);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Could not remove id = " + id);
                        }
                    }
                }

                if (DEBUG_LOADERS) {
                    Log.d(TAG, "loaded workspace in " + (SystemClock.uptimeMillis() - t) + "ms");
                    Log.d(TAG, "workspace layout: ");
                    for (int y = 0; y < mCellCountY; y++) {
                        String line = "";
                        for (int s = 0; s < Launcher.SCREEN_COUNT; s++) {
                            if (s > 0) {
                                line += " | ";
                            }
                            for (int x = 0; x < mCellCountX; x++) {
                                line += ((occupied[s][x][y] != null) ? "#" : ".");
                            }
                        }
                        Log.d(TAG, "[ " + line + " ]");
                    }
                }
            }
        }

        /**
         * Filters the set of items who are directly or indirectly (via another container) on the
         * specified screen.
         */
        private void filterCurrentWorkspaceItems(int currentScreen, ArrayList<ItemInfo> allWorkspaceItems, ArrayList<ItemInfo> currentScreenItems, ArrayList<ItemInfo> otherScreenItems) {
            // Purge any null ItemInfos
            Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
            while (iter.hasNext()) {
                ItemInfo i = iter.next();
                if (i == null) {
                    iter.remove();
                }
            }

            // If we aren't filtering on a screen, then the set of items to load is the full set of
            // items given.
            if (currentScreen < 0) {
                currentScreenItems.addAll(allWorkspaceItems);
            }

            // Order the set of items by their containers first, this allows use to walk through the
            // list sequentially, build up a list of containers that are in the specified screen,
            // as well as all items in those containers.
            Set<Long> itemsOnScreen = new HashSet<Long>();
            Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return (int) (lhs.container - rhs.container);
                }
            });
            for (ItemInfo info : allWorkspaceItems) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    if (info.screen == currentScreen) {
                        currentScreenItems.add(info);
                        itemsOnScreen.add(info.id);
                    } else {
                        otherScreenItems.add(info);
                    }
                } else if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    if (itemsOnScreen.contains(info.container)) {
                        currentScreenItems.add(info);
                        itemsOnScreen.add(info.id);
                    } else {
                        otherScreenItems.add(info);
                    }
                }
            }
        }

        /**
         * Filters the set of widgets which are on the specified screen.
         */
        private void filterCurrentAppWidgets(int currentScreen, ArrayList<LauncherAppWidgetInfo> appWidgets, ArrayList<LauncherAppWidgetInfo> currentScreenWidgets, ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {
            // If we aren't filtering on a screen, then the set of items to load is the full set of
            // widgets given.
            if (currentScreen < 0) {
                currentScreenWidgets.addAll(appWidgets);
            }

            for (LauncherAppWidgetInfo widget : appWidgets) {
                if (widget == null) continue;
                if (widget.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && widget.screen == currentScreen) {
                    currentScreenWidgets.add(widget);
                } else {
                    otherScreenWidgets.add(widget);
                }
            }
        }

        /**
         * Filters the set of folders which are on the specified screen.
         */
        private void filterCurrentFolders(int currentScreen, HashMap<Long, ItemInfo> itemsIdMap, HashMap<Long, FolderInfo> folders, HashMap<Long, FolderInfo> currentScreenFolders, HashMap<Long, FolderInfo> otherScreenFolders) {
            // If we aren't filtering on a screen, then the set of items to load is the full set of
            // widgets given.
            if (currentScreen < 0) {
                currentScreenFolders.putAll(folders);
            }

            for (long id : folders.keySet()) {
                ItemInfo info = itemsIdMap.get(id);
                FolderInfo folder = folders.get(id);
                if (info == null || folder == null) continue;
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && info.screen == currentScreen) {
                    currentScreenFolders.put(id, folder);
                } else {
                    otherScreenFolders.put(id, folder);
                }
            }
        }

        /**
         * Sorts the set of items by hotseat, workspace (spatially from top to bottom, left to
         * right)
         */
        private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
            // XXX: review this
            Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    int cellCountX = LauncherModel.getCellCountX();
                    int cellCountY = LauncherModel.getCellCountY();
                    int screenOffset = cellCountX * cellCountY;
                    int containerOffset = screenOffset * (Launcher.SCREEN_COUNT + 1); // +1 hotseat
                    long lr = (lhs.container * containerOffset + lhs.screen * screenOffset + lhs.cellY * cellCountX + lhs.cellX);
                    long rr = (rhs.container * containerOffset + rhs.screen * screenOffset + rhs.cellY * cellCountX + rhs.cellX);
                    return (int) (lr - rr);
                }
            });
        }

        private void bindWorkspaceItems(final Callbacks oldCallbacks, final ArrayList<ItemInfo> workspaceItems, final ArrayList<LauncherAppWidgetInfo> appWidgets, final HashMap<Long, FolderInfo> folders, ArrayList<Runnable> deferredBindRunnables) {

            final boolean postOnMainThread = (deferredBindRunnables != null);

            // Bind the workspace items
            int N = workspaceItems.size();
            for (int i = 0; i < N; i += ITEMS_CHUNK) {
                final int start = i;
                final int chunkSize = (i + ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N - i);
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindItems(workspaceItems, start, start + chunkSize);
                        }
                    }
                };
                if (postOnMainThread) {
                    deferredBindRunnables.add(r);
                } else {
                    runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                }
            }

            // Bind the folders
            if (!folders.isEmpty()) {
                final Runnable r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindFolders(folders);
                        }
                    }
                };
                if (postOnMainThread) {
                    deferredBindRunnables.add(r);
                } else {
                    runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                }
            }

            // Bind the widgets, one at a time
            N = appWidgets.size();
            for (int i = 0; i < N; i++) {
                final LauncherAppWidgetInfo widget = appWidgets.get(i);
                final Runnable r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindAppWidget(widget);
                        }
                    }
                };
                if (postOnMainThread) {
                    deferredBindRunnables.add(r);
                } else {
                    runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                }
            }
        }

        /**
         * Binds all loaded data to actual views on the main thread.
         */
        private void bindWorkspace(int synchronizeBindPage) {
            final long t = SystemClock.uptimeMillis();
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            final boolean isLoadingSynchronously = (synchronizeBindPage > -1);
            final int currentScreen = isLoadingSynchronously ? synchronizeBindPage : oldCallbacks.getCurrentWorkspaceScreen();

            // Load all the items that are on the current page first (and in the process, unbind
            // all the existing workspace items before we call startBinding() below.
            unbindWorkspaceItemsOnMainThread();
            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                folders.putAll(sBgFolders);
                itemsIdMap.putAll(sBgItemsIdMap);
            }

            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<Long, FolderInfo>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<Long, FolderInfo>();

            // Separate the items that are on the current screen, and all the other remaining items
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems, otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets, otherAppWidgets);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders, otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };
            runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);

            // Load items on the current page
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets, currentFolders, null);
            if (isLoadingSynchronously) {
                r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.onPageBoundSynchronously(currentScreen);
                        }
                    }
                };
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            // Load all the remaining pages (if we are loading synchronously, we want to defer this
            // work until after the first render)
            mDeferredBindRunnables.clear();
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders, (isLoadingSynchronously ? mDeferredBindRunnables : null));

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }

                    // If we're profiling, ensure this is the last thing in the queue.
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound workspace in " + (SystemClock.uptimeMillis() - t) + "ms");
                    }

                    mIsLoadingAndBindingWorkspace = false;
                }
            };
            if (isLoadingSynchronously) {
                mDeferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        private void loadAndBindAllApps() {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindAllApps mAllAppsLoaded=" + mAllAppsLoaded);
            }
            if (!mAllAppsLoaded) {
                loadAllAppsByBatch();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            } else {
                onlyBindAllApps();
            }
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }

            // shallow copy
            @SuppressWarnings("unchecked") final ArrayList<ApplicationInfo> list = (ArrayList<ApplicationInfo>) mBgAllAppsList.data.clone();
            Runnable r = new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                    }
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound all " + list.size() + " apps from cache in " + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                }
            };
            boolean isRunningOnMainThread = !(sWorkerThread.getThreadId() == Process.myTid());
            if (oldCallbacks.isAllAppsVisible() && isRunningOnMainThread) {
                r.run();
            } else {
                mHandler.post(r);
            }
        }

        public boolean isHidePackage(String pn) {
            Log.d(TAG, "LauncherModel:" + pn + ":" + AppConfig.isHidePackage(pn));
            return AppConfig.isHidePackage(pn);
        }

        private void loadAllAppsByBatch() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<UserHandle> profiles = mUserManager.getUserProfiles();

            mBgAllAppsList.clear();
            final int profileCount = profiles.size();
            for (int p = 0; p < profileCount; p++) {
                UserHandle user = profiles.get(p);
                List<LauncherActivityInfo> apps = null;
                int N = Integer.MAX_VALUE;

                int startIndex;
                int i = 0;
                int batchSize = -1;
                while (i < N && !mStopped) {
                    if (i == 0) {
                        final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                        apps = mLauncherApps.getActivityList(null, user);
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "queryIntentActivities took " + (SystemClock.uptimeMillis() - qiaTime) + "ms");
                        }
                        if (apps == null) {
                            return;
                        }
                        N = apps.size();
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "queryIntentActivities got " + N + " apps");
                        }
                        if (N == 0) {
                            // There are no apps?!?
                            return;
                        }
                        if (mBatchSize == 0) {
                            batchSize = N;
                        } else {
                            batchSize = mBatchSize;
                        }

                        final long sortTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                        Collections.sort(apps, new LauncherModel.ShortcutNameComparator(mLabelCache));
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "sort took " + (SystemClock.uptimeMillis() - sortTime) + "ms");
                        }
                    }

                    final long t2 = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                    final ArrayList<ApplicationInfo> mBgRemoveHide = new ArrayList<ApplicationInfo>();
                    final ArrayList<String> remove2 = new ArrayList<String>();
                    mBgRemoveHide.clear();

                    startIndex = i;
                    for (int j = 0; i < N && j < batchSize; j++) {
                        // This builds the icon bitmaps.

                        if (!isHidePackage(apps.get(i).getName())) {
                            ApplicationInfo ai = new ApplicationInfo(apps.get(i), user, mIconCache, mLabelCache);
                            ai.title = FixScreenShortcut.getAppAlternativeTitle(mContext, ai.componentName, ai.title);
                            mBgAllAppsList.add(ai);
                        } else {
                            mBgRemoveHide.add(new ApplicationInfo(apps.get(i), user, mIconCache, mLabelCache));

                            //                    		Log.d("allen","!!!!!!!!user::"+ user);
                            remove2.add(apps.get(i).getComponentName().getPackageName());

                        }
                        i++;
                    }

                    Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                    if (cb != null) {
                        cb.bindComponentsRemoved(remove2, mBgRemoveHide, false, user);
                    }

                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    final ArrayList<ApplicationInfo> added = mBgAllAppsList.added;
                    final boolean firstProfile = p == 0;
                    mBgAllAppsList.added = new ArrayList<ApplicationInfo>();
                    mHandler.post(new Runnable() {
                        public void run() {
                            final long t = SystemClock.uptimeMillis();
                            if (callbacks != null) {
                                if (firstProfile) {
                                    callbacks.bindAllApplications(added);
                                } else {
                                    callbacks.bindAppsAdded(added);
                                }
                                if (DEBUG_LOADERS) {
                                    Log.d(TAG, "bound " + added.size() + " apps in " + (SystemClock.uptimeMillis() - t) + "ms");
                                }
                            } else {
                                Log.i(TAG, "not binding apps: no Launcher activity");
                            }
                        }
                    });

                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "batch of " + (i - startIndex) + " icons processed in " + (SystemClock.uptimeMillis() - t2) + "ms");
                    }

                    if (mAllAppsLoadDelay > 0 && i < N) {
                        try {
                            if (DEBUG_LOADERS) {
                                Log.d(TAG, "sleeping for " + mAllAppsLoadDelay + "ms");
                            }
                            Thread.sleep(mAllAppsLoadDelay);
                        } catch (InterruptedException exc) {
                        }
                    }
                }

                if (DEBUG_LOADERS) {
                    Log.d(TAG, "cached all " + N + " apps in " + (SystemClock.uptimeMillis() - t) + "ms" + (mAllAppsLoadDelay > 0 ? " (including delay)" : ""));
                }
            }
        }

        public void dumpState() {
            synchronized (sBgLock) {
                Log.d(TAG, "mLoaderTask.mContext=" + mContext);
                Log.d(TAG, "mLoaderTask.mIsLaunching=" + mIsLaunching);
                Log.d(TAG, "mLoaderTask.mStopped=" + mStopped);
                Log.d(TAG, "mLoaderTask.mLoadAndBindStepFinished=" + mLoadAndBindStepFinished);
                Log.d(TAG, "mItems size=" + sBgWorkspaceItems.size());
            }
        }
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;
        UserHandle mUser;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted

        public PackageUpdatedTask(int op, String[] packages, UserHandle user) {
            mOp = op;
            mPackages = packages;
            mUser = user;
        }

        public void run() {
            final Context context = mApp;

            final String[] packages = mPackages;
            final int N = packages.length;
            switch (mOp) {
                case OP_ADD:
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.addPackage " + packages[i]);
                        mBgAllAppsList.addPackage(context, packages[i], mUser);
                    }
                    break;
                case OP_UPDATE:
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.updatePackage " + packages[i]);
                        mBgAllAppsList.updatePackage(context, packages[i], mUser);
                        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(app.getWidgetPreviewCacheDb(), packages[i]);
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        mBgAllAppsList.removePackage(packages[i], mUser);
                        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(app.getWidgetPreviewCacheDb(), packages[i]);
                    }
                    break;
            }

            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            final ArrayList<ApplicationInfo> removedApps = new ArrayList<ApplicationInfo>();

            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<ApplicationInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<ApplicationInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }

            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            if (callbacks == null) {
                Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }

            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsAdded(addedFinal);
                        }
                    }
                });
            }
            if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }
            // If a package has been removed, or an app has been removed as a result of
            // an update (for example), make the removed callback.
            if (mOp == OP_REMOVE || !removedApps.isEmpty()) {
                final boolean permanent = (mOp == OP_REMOVE);
                final ArrayList<String> removedPackageNames = new ArrayList<String>(Arrays.asList(packages));

                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindComponentsRemoved(removedPackageNames, removedApps, permanent, mUser);
                        }
                    }
                });
            }

            final ArrayList<Object> widgetsAndShortcuts = getSortedWidgetsAndShortcuts(context);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                    if (callbacks == cb && cb != null) {
                        callbacks.bindPackagesUpdated(widgetsAndShortcuts);
                    }
                }
            });
        }
    }

    // Returns a list of ResolveInfos/AppWindowInfos in sorted order
    public static ArrayList<Object> getSortedWidgetsAndShortcuts(Context context) {
        ArrayList<Object> widgetsAndShortcuts = new ArrayList<Object>();

        // Get all user profiles.
        AppWidgetManager widgetManager = (AppWidgetManager) context.getSystemService(Context.APPWIDGET_SERVICE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        List<UserHandle> profiles = userManager.getUserProfiles();

        // Add the widgets for the managed profiles next.
        final int profileCount = profiles.size();
        for (int i = 0; i < profileCount; i++) {
            UserHandle profile = profiles.get(i);
            // Add the widget providers for the profile.
            List<AppWidgetProviderInfo> providers = widgetManager.getInstalledProvidersForProfile(profile);
            widgetsAndShortcuts.addAll(providers);
        }

        // Add all shortcuts for the user.
        PackageManager packageManager = context.getPackageManager();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        widgetsAndShortcuts.addAll(packageManager.queryIntentActivities(shortcutsIntent, 0));

        Collections.sort(widgetsAndShortcuts, new LauncherModel.WidgetAndShortcutNameComparator(packageManager));

        return widgetsAndShortcuts;
    }

    /**
     * This is called from the code that adds shortcuts from the intent receiver.  This
     * doesn't have a Cursor, but
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, UserHandle user, Context context) {
        return getShortcutInfo(manager, intent, user, context, null, -1, -1, null);
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     * <p>
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, UserHandle user, Context context, Cursor c, int iconIndex, int titleIndex, HashMap<Object, CharSequence> labelCache) {
        Bitmap icon = null;
        final ShortcutInfo info = new ShortcutInfo();
        info.user = user;

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }

        LauncherActivityInfo lai = mLauncherApps.resolveActivity(intent, user);
        if (lai == null) {
            return null;
        }

        icon = mIconCache.getIcon(componentName, lai, labelCache);
        // the db
        if (icon == null) {
            if (c != null) {
                icon = getIconFromCursor(c, iconIndex, context);
            }
        }
        // the fallback icon
        if (icon == null) {
            icon = getFallbackIcon();
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);

        // from the resource
        ComponentName key = lai.getComponentName();
        if (labelCache != null && labelCache.containsKey(key)) {
            info.title = labelCache.get(key);
        } else {
            info.title = lai.getLabel();
            if (labelCache != null) {
                labelCache.put(key, info.title);
            }
        }
        // from the db
        if (info.title == null) {
            if (c != null) {
                info.title = c.getString(titleIndex);
            }
        }
        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }

        info.contentDescription = mApp.getPackageManager().getUserBadgedLabel(info.title, user);
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

        info.title = FixScreenShortcut.getAppAlternativeTitle(context, componentName, info.title);

        return info;
    }

    /**
     * Returns the set of workspace ShortcutInfos with the specified intent.
     */
    static ArrayList<ItemInfo> getWorkspaceShortcutItemInfosWithIntent(Intent intent) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgWorkspaceItems) {
                if (info instanceof ShortcutInfo) {
                    ShortcutInfo shortcut = (ShortcutInfo) info;
                    if (shortcut.intent.toUri(0).equals(intent.toUri(0))) {
                        items.add(shortcut);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    private ShortcutInfo getShortcutInfo(Cursor c, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex, int titleIndex) {

        Bitmap icon = null;
        final ShortcutInfo info = new ShortcutInfo();
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;

        // TODO: If there's an explicit component and we can't install that, delete it.

        info.title = c.getString(titleIndex);
        info.contentDescription = mApp.getPackageManager().getUserBadgedLabel(info.title, info.user);

        int iconType = c.getInt(iconTypeIndex);
        switch (iconType) {
            case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                info.customIcon = false;
                // the resource
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    if (resources != null) {
                        final int id = resources.getIdentifier(resourceName, null, null);

                        //                    	Log.d("allen", "4");

                        boolean isNeedBg = !AppConfig.isNoNeedLauncherBackground(packageName);
                        if (Utilities.mSystemUI != null && (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(Utilities.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(Utilities.mSystemUI))) {
                            isNeedBg = false;
                        }

                        icon = Utilities.createIconBitmap(mIconCache.getFullResIcon(resources, id, Process.myUserHandle()), context, isNeedBg);
                    }
                } catch (Exception e) {
                    // drop this. we have other places to look for icons
                }
                // the db
                if (icon == null) {
                    icon = getIconFromCursor(c, iconIndex, context);
                }
                // the fallback icon
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.usingFallbackIcon = true;
                }
                break;
            case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
                icon = getIconFromCursor(c, iconIndex, context);
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.customIcon = false;
                    info.usingFallbackIcon = true;
                } else {
                    info.customIcon = true;
                }
                break;
            default:
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
                info.customIcon = false;
                break;
        }
        info.setIcon(icon);
        return info;
    }

    Bitmap getIconFromCursor(Cursor c, int iconIndex, Context context) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            Log.d(TAG, "getIconFromCursor app=" + c.getString(c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
        }
        byte[] data = c.getBlob(iconIndex);
        try {

            //        	Log.d("allen", "3");
            //        	boolean isNeedBg = !AppConfig.isNoNeedLauncherBackground(packageName);
            return Utilities.createIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), context);
        } catch (Exception e) {
            return null;
        }
    }

    ShortcutInfo addShortcut(Context context, Intent data, long container, int screen, int cellX, int cellY, boolean notify) {
        final ShortcutInfo info = infoFromShortcutIntent(context, data, null);
        if (info == null) {
            return null;
        }
        addItemToDatabase(context, info, container, screen, cellX, cellY, notify);

        return info;
    }

    ShortcutInfo addShortcutFix(Context context, Intent data, long container, int screen, int cellX, int cellY, boolean notify) {
        final ShortcutInfo info = infoFromShortcutIntent(context, data, null);
        if (info == null) {
            return null;
        }
        info.itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
        addItemToDatabase(context, info, container, screen, cellX, cellY, notify);

        return info;
    }

    /**
     * Attempts to find an AppWidgetProviderInfo that matches the given component.
     */
    AppWidgetProviderInfo findAppWidgetProviderInfoWithComponent(Context context, ComponentName component) {
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(component)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Returns a list of all the widgets that can handle configuration with a particular mimeType.
     */
    List<WidgetMimeTypeHandlerData> resolveWidgetsForMimeType(Context context, String mimeType) {
        final PackageManager packageManager = context.getPackageManager();
        final List<WidgetMimeTypeHandlerData> supportedConfigurationActivities = new ArrayList<WidgetMimeTypeHandlerData>();

        final Intent supportsIntent = new Intent(InstallWidgetReceiver.ACTION_SUPPORTS_CLIPDATA_MIMETYPE);
        supportsIntent.setType(mimeType);

        // Create a set of widget configuration components that we can test against
        final List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        final HashMap<ComponentName, AppWidgetProviderInfo> configurationComponentToWidget = new HashMap<ComponentName, AppWidgetProviderInfo>();
        for (AppWidgetProviderInfo info : widgets) {
            configurationComponentToWidget.put(info.configure, info);
        }

        // Run through each of the intents that can handle this type of clip data, and cross
        // reference them with the components that are actual configuration components
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(supportsIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : activities) {
            final ActivityInfo activityInfo = info.activityInfo;
            final ComponentName infoComponent = new ComponentName(activityInfo.packageName, activityInfo.name);
            if (configurationComponentToWidget.containsKey(infoComponent)) {
                supportedConfigurationActivities.add(new InstallWidgetReceiver.WidgetMimeTypeHandlerData(info, configurationComponentToWidget.get(infoComponent)));
            }
        }
        return supportedConfigurationActivities;
    }

    ShortcutInfo infoFromShortcutIntent(Context context, Intent data, Bitmap fallbackIcon) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        UserHandle user = data.getParcelableExtra(ItemInfo.EXTRA_PROFILE);
        if (user == null) user = Process.myUserHandle();

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }

        Bitmap icon = null;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;


        if (bitmap != null && bitmap instanceof Bitmap) {
            //        	Log.d("allen", "1");
            icon = Utilities.createIconBitmap(new FastBitmapDrawable((Bitmap) bitmap), context, true);
            customIcon = true;
        } else {

            //        	Log.d("allen", "2");
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);


                    //                	Log.d("allen", "2");
                    icon = Utilities.createIconBitmap(mIconCache.getFullResIcon(resources, id, user), context, true);
                } catch (Exception e) {
                    Log.w(TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }

        final ShortcutInfo info = new ShortcutInfo();

        if (icon == null) {
            if (fallbackIcon != null) {
                icon = fallbackIcon;
            } else {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
        }
        info.setIcon(icon);

        info.title = name;
        info.contentDescription = mApp.getPackageManager().getUserBadgedLabel(name, info.user);
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;

        return info;
    }

    boolean queueIconToBeChecked(HashMap<Object, byte[]> cache, ShortcutInfo info, Cursor c, int iconIndex) {
        // If apps can't be on SD, don't even bother.
        if (!mAppsCanBeOnRemoveableStorage) {
            return false;
        }
        // If this icon doesn't have a custom icon, check to see
        // what's stored in the DB, and if it doesn't match what
        // we're going to show, store what we are going to show back
        // into the DB.  We do this so when we're loading, if the
        // package manager can't find an icon (for example because
        // the app is on SD) then we can use that instead.
        if (!info.customIcon && !info.usingFallbackIcon) {
            cache.put(info, c.getBlob(iconIndex));
            return true;
        }
        return false;
    }

    void updateSavedIcon(Context context, ShortcutInfo info, byte[] data) {
        boolean needSave = false;
        try {
            if (data != null) {
                Bitmap saved = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap loaded = info.getIcon(mIconCache);
                needSave = !saved.sameAs(loaded);
            } else {
                needSave = true;
            }
        } catch (Exception e) {
            needSave = true;
        }
        if (needSave) {
            Log.d(TAG, "going to save icon bitmap for info=" + info);
            // This is slower than is ideal, but this only happens once
            // or when the app is updated with a new icon.
            updateItemInDatabase(context, info);
        }
    }

    /**
     * Return an existing FolderInfo object if we have encountered this ID previously,
     * or make a new one.
     */
    private static FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
            // No placeholder -- create a new instance
            folderInfo = new FolderInfo();
            folders.put(id, folderInfo);
        }
        return folderInfo;
    }

    public static final Comparator<ApplicationInfo> getAppNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<ApplicationInfo>() {
            public final int compare(ApplicationInfo a, ApplicationInfo b) {
                if (a.user.equals(b.user)) {
                    int result = collator.compare(a.title.toString(), b.title.toString());
                    if (result == 0) {
                        result = a.componentName.compareTo(b.componentName);
                    }
                    return result;
                } else {
                    // TODO: Order this based on profile type rather than string compares.
                    return a.user.toString().compareTo(b.user.toString());
                }
            }
        };
    }

    public static final Comparator<ApplicationInfo> APP_INSTALL_TIME_COMPARATOR = new Comparator<ApplicationInfo>() {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            if (a.firstInstallTime < b.firstInstallTime) return 1;
            if (a.firstInstallTime > b.firstInstallTime) return -1;
            return 0;
        }
    };

    public static final Comparator<AppWidgetProviderInfo> getWidgetNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<AppWidgetProviderInfo>() {
            public final int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                return collator.compare(a.label.toString(), b.label.toString());
            }
        };
    }

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

    public static class ShortcutNameComparator implements Comparator<LauncherActivityInfo> {
        private Collator mCollator;
        private HashMap<Object, CharSequence> mLabelCache;

        ShortcutNameComparator() {
            mLabelCache = new HashMap<Object, CharSequence>();
            mCollator = Collator.getInstance();
        }

        ShortcutNameComparator(HashMap<Object, CharSequence> labelCache) {
            mLabelCache = labelCache;
            mCollator = Collator.getInstance();
        }

        @Override
        public final int compare(LauncherActivityInfo a, LauncherActivityInfo b) {
            CharSequence labelA, labelB;
            ComponentName keyA = a.getComponentName();
            ComponentName keyB = b.getComponentName();
            if (mLabelCache.containsKey(keyA)) {
                labelA = mLabelCache.get(keyA);
            } else {
                labelA = a.getLabel().toString();

                mLabelCache.put(keyA, labelA);
            }
            if (mLabelCache.containsKey(keyB)) {
                labelB = mLabelCache.get(keyB);
            } else {
                labelB = b.getLabel().toString();

                mLabelCache.put(keyB, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    }

    ;

    public static class WidgetAndShortcutNameComparator implements Comparator<Object> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, String> mLabelCache;

        WidgetAndShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, String>();
            mCollator = Collator.getInstance();
        }

        public final int compare(Object a, Object b) {
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = (a instanceof AppWidgetProviderInfo) ? ((AppWidgetProviderInfo) a).loadLabel(mPackageManager) : ((ResolveInfo) a).loadLabel(mPackageManager).toString();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = (b instanceof AppWidgetProviderInfo) ? ((AppWidgetProviderInfo) b).loadLabel(mPackageManager) : ((ResolveInfo) b).loadLabel(mPackageManager).toString();
                mLabelCache.put(b, labelB);
            }
            final int compareResult = mCollator.compare(labelA, labelB);
            if (compareResult != 0) {
                return compareResult;
            }
            return 0;
        }
    }

    ;

    public void dumpState() {
        Log.d(TAG, "mCallbacks=" + mCallbacks);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", mBgAllAppsList.data);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", mBgAllAppsList.added);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", mBgAllAppsList.removed);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", mBgAllAppsList.modified);
        if (mLoaderTask != null) {
            mLoaderTask.dumpState();
        } else {
            Log.d(TAG, "mLoaderTask=null");
        }
    }
}
