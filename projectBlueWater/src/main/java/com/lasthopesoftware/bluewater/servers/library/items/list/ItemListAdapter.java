package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.menu.ItemMenu;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewChangedListener;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	private ViewAnimator shownMenu;

	private OnViewChangedListener onViewChangedListener;
	private Runnable onAnyMenuShown;
	private Runnable onAllMenusHidden;

	private int numberOfMenusShown;

	private final OnViewChangedListener onViewChangedListenerWrapper = new OnViewChangedListener() {
		@Override
		public void onViewChanged(ViewAnimator viewAnimator) {
			onViewChangedListener.onViewChanged(viewAnimator);

			if (viewAnimator.getDisplayedChild() > 0) {
				if (numberOfMenusShown == 0 && onAnyMenuShown != null)
					onAnyMenuShown.run();

				++numberOfMenusShown;

				if (shownMenu != null)
					LongClickViewAnimatorListener.tryFlipToPreviousView(shownMenu);

				shownMenu = viewAnimator;
			} else if (--numberOfMenusShown == 0 && onAllMenusHidden != null) {
				onAllMenusHidden.run();
			}
		}
	};

	public ItemListAdapter(Activity activity, int resource, List<T> items) {
		super(activity, resource, items);
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator = ItemMenu.getView(getItem(position), convertView, parent);

		if (convertView == null)
			notifyOnFlipViewAnimator.setViewChangedListener(onViewChangedListenerWrapper);

		return notifyOnFlipViewAnimator;
	}

	public void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
		this.onViewChangedListener = onViewChangedListener;
	}

	public void setOnAnyMenuShown(Runnable onAnyMenuShown) {
		this.onAnyMenuShown = onAnyMenuShown;
	}

	public void setOnAllMenusHidden(Runnable onAllMenusHidden) {
		this.onAllMenusHidden = onAllMenusHidden;
	}
}
