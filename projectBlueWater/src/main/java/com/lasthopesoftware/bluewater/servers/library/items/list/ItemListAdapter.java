package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.menu.ItemMenu;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewChangedListener;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	private OnViewChangedListener onViewChangedListener;

	private final OnViewChangedListener onViewChangedListenerWrapper = new OnViewChangedListener() {
		@Override
		public void onViewChanged(ViewAnimator viewAnimator) {
			onViewChangedListener.onViewChanged(viewAnimator);
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
}
