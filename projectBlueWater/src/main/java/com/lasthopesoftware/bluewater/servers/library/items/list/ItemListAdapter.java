package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.menu.ItemMenu;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.ViewChangedHandler;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	private final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();

	public ItemListAdapter(Activity activity, int resource, List<T> items) {
		super(activity, resource, items);
	}

	public ItemListAdapter(Activity activity, int resource, List<T> items, IItemListMenuChangeHandler itemListMenuEvents) {
		this(activity, resource, items);

		viewChangedHandler.setOnAllMenusHidden(itemListMenuEvents);
		viewChangedHandler.setOnAnyMenuShown(itemListMenuEvents);
		viewChangedHandler.setOnViewChangedListener(itemListMenuEvents);
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator = ItemMenu.getView(getItem(position), convertView, parent);

		if (convertView == null)
			notifyOnFlipViewAnimator.setViewChangedListener(viewChangedHandler);

		return notifyOnFlipViewAnimator;
	}
}
