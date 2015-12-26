package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

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
		LibrarySession.GetActiveLibrary(context, new TwoParameterRunnable<FluentTask<Integer,Void,Library>, Library>() {
			@Override
			public void run(FluentTask<Integer, Void, Library> owner, final Library library) {
				if (!v.isShown()) return;

				final StoredItemAccess syncListManager = new StoredItemAccess(context, library);
				syncListManager.isItemMarkedForSync(item, new TwoParameterRunnable<FluentTask<Void,Void,Boolean>, Boolean>() {
					@Override
					public void run(FluentTask<Void, Void, Boolean> owner, final Boolean isSynced) {
						if (!v.isShown()) return;

						syncButton.setImageDrawable(ViewUtils.getDrawable(context, isSynced ? R.drawable.ic_sync_on : R.drawable.ic_sync_off));
						syncButton.setOnClickListener(new SyncFilesClickHandler(notifyOnFlipViewAnimator, library, item, isSynced));
						syncButton.setEnabled(true);
					}
				});
			}
		});
	}
}
