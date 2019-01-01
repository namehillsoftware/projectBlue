package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.sync.SyncWorker;

/**
 * Created by david on 7/18/15.
 */
public class SyncFilesClickHandler extends  AbstractMenuClickHandler {
	private final StoredItemAccess mSyncListManager;
	private boolean mIsSynced;
	private final IItem mItem;

	public SyncFilesClickHandler(NotifyOnFlipViewAnimator menuContainer, Library library, IItem item, boolean isSynced) {
		super(menuContainer);

		mItem = item;
		mIsSynced = isSynced;
		mSyncListManager = new StoredItemAccess(menuContainer.getContext(), library);
	}

	@Override
	public void onClick(View v) {
		mIsSynced = !mIsSynced;
		mSyncListManager.toggleSync(mItem, mIsSynced);

		SyncWorker.syncImmediately();

		super.onClick(v);
	}
}
