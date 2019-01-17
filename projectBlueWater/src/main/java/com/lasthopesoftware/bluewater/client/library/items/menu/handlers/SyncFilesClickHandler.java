package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;

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

		StoredSyncService.doSync(v.getContext());

		super.onClick(v);
	}
}
