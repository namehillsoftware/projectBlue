package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;

/**
 * Created by david on 7/18/15.
 */
public class SyncFilesClickHandler extends  AbstractMenuClickHandler {
	private final StoredItemAccess syncListManager;
	private boolean isSynced;
	private final LibraryId libraryId;
	private final IItem item;

	SyncFilesClickHandler(NotifyOnFlipViewAnimator menuContainer, LibraryId libraryId, IItem item, boolean isSynced) {
		super(menuContainer);
		this.libraryId = libraryId;
		this.item = item;
		this.isSynced = isSynced;
		syncListManager = new StoredItemAccess(menuContainer.getContext());
	}

	@Override
	public void onClick(View v) {
		isSynced = !isSynced;
		syncListManager.toggleSync(libraryId, item, isSynced);

		StoredSyncService.doSyncUninterruptedFromUiThread(v.getContext());

		super.onClick(v);
	}
}
