package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.threading.IFluentTask;

/**
 * Created by david on 8/16/15.
 */
public class SyncFilesIsVisibleHandler implements View.OnLayoutChangeListener {

	private static Drawable mSyncOnDrawable;

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

		v.removeOnLayoutChangeListener(this);

		final Context context = notifyOnFlipViewAnimator.getContext();
		LibrarySession.GetActiveLibrary(context, new IFluentTask.OnCompleteListener<Integer, Void, Library>() {
			@Override
			public void onComplete(IFluentTask<Integer, Void, Library> owner, final Library library) {
				final StoredItemAccess syncListManager = new StoredItemAccess(context, library);
				syncListManager.isItemMarkedForSync(item, new IFluentTask.OnCompleteListener<Void, Void, Boolean>() {
					@Override
					public void onComplete(IFluentTask<Void, Void, Boolean> owner, final Boolean isSynced) {
						if (isSynced)
							syncButton.setImageDrawable(getSyncOnDrawable(notifyOnFlipViewAnimator.getContext()));

						syncButton.setOnClickListener(new SyncFilesClickHandler(notifyOnFlipViewAnimator, library, item, isSynced));
						syncButton.setEnabled(true);
					}
				});
			}
		});
	}

	private static Drawable getSyncOnDrawable(Context context) {
		if (mSyncOnDrawable == null)
			mSyncOnDrawable = context.getResources().getDrawable(R.drawable.ic_sync_on);

		return mSyncOnDrawable;
	}
}
