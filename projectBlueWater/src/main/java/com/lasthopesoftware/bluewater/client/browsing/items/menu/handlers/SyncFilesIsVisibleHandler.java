package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers;

import android.view.View;
import android.widget.ImageButton;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

public class SyncFilesIsVisibleHandler implements View.OnLayoutChangeListener {

	private final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator;
	private final ImageButton syncButton;
	private final StoredItemAccess storedItemAccess;
	private final LibraryId libraryId;
	private final IItem item;

	public SyncFilesIsVisibleHandler(NotifyOnFlipViewAnimator notifyOnFlipViewAnimator, ImageButton syncButton, StoredItemAccess storedItemAccess, LibraryId libraryId, IItem item) {
		this.notifyOnFlipViewAnimator = notifyOnFlipViewAnimator;
		this.syncButton = syncButton;
		this.storedItemAccess = storedItemAccess;
		this.libraryId = libraryId;
		this.item = item;
	}

	@Override
	public void onLayoutChange(final View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if (!v.isShown()) return;

		storedItemAccess
			.isItemMarkedForSync(libraryId, item)
			.eventually(LoopedInPromise.response(isSynced -> {
				if (!v.isShown()) return null;

				syncButton.setImageDrawable(ViewUtils.getDrawable(v.getContext(), isSynced ? R.drawable.ic_sync_on : R.drawable.ic_sync_off));
				syncButton.setOnClickListener(new SyncFilesClickHandler(notifyOnFlipViewAnimator, libraryId, item, isSynced));
				syncButton.setEnabled(true);

				return null;
			}, v.getContext()));
	}
}
