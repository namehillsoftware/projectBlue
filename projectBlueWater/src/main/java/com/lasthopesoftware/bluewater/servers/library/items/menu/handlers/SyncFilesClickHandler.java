package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service.ItemSyncService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;

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

		ItemSyncService.doSync(v.getContext());

		super.onClick(v);
	}
}
