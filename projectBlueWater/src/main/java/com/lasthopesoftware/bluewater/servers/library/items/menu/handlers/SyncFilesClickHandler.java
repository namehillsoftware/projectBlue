package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.view.View;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.MainApplication;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;

/**
 * Created by david on 7/18/15.
 */
public class SyncFilesClickHandler extends  AbstractMenuClickHandler {
	private final StoredItemAccess mSyncListManager;
	private boolean mIsSynced;
	private final IItem mItem;

	public SyncFilesClickHandler(ViewFlipper menuContainer, IItem item, boolean isSynced) {
		super(menuContainer);

		mItem = item;
		mIsSynced = isSynced;
		mSyncListManager = new StoredItemAccess(menuContainer.getContext());
	}

	@Override
	public void onClick(View v) {
		mIsSynced = !mIsSynced;
		mSyncListManager.toggleSync(mItem, mIsSynced);

		if (MainApplication.DEBUG_MODE) // For development purposes only
			mSyncListManager.startSync();

		super.onClick(v);
	}
}
