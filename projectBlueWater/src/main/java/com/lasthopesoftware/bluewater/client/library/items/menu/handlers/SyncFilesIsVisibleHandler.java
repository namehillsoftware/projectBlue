package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

/**
 * Created by david on 8/16/15.
 */
public class SyncFilesIsVisibleHandler implements View.OnLayoutChangeListener {

	private final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator;
	private final ImageButton syncButton;
	private final IItem item;

	public SyncFilesIsVisibleHandler(NotifyOnFlipViewAnimator notifyOnFlipViewAnimator, ImageButton syncButton, IItem item) {
		this.notifyOnFlipViewAnimator = notifyOnFlipViewAnimator;
		this.syncButton = syncButton;
		this.item = item;
	}

	@Override
	public void onLayoutChange(final View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if (!v.isShown()) return;

		final Context context = v.getContext();
		LibrarySession.GetActiveLibrary(context, library -> {
			if (!v.isShown()) return;

			final StoredItemAccess syncListManager = new StoredItemAccess(context, library);
			syncListManager.isItemMarkedForSync(item)
				.onComplete((isSynced) -> {
					if (!v.isShown()) return;

					syncButton.setImageDrawable(ViewUtils.getDrawable(context, isSynced ? R.drawable.ic_sync_on : R.drawable.ic_sync_off));
					syncButton.setOnClickListener(new SyncFilesClickHandler(notifyOnFlipViewAnimator, library, item, isSynced));
					syncButton.setEnabled(true);
				});
		});
	}
}
