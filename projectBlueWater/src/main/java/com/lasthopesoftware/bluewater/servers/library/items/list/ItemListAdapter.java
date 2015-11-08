package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.menu.ListItemMenuBuilder;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.ViewChangedHandler;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	private final ListItemMenuBuilder listItemMenuBuilder = new ListItemMenuBuilder();

	public ItemListAdapter(Activity activity, int resource, List<T> items) {
		super(activity, resource, items);
	}

	public ItemListAdapter(Activity activity, int resource, List<T> items, IItemListMenuChangeHandler itemListMenuEvents) {
		this(activity, resource, items);

		final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
		viewChangedHandler.setOnAllMenusHidden(itemListMenuEvents);
		viewChangedHandler.setOnAnyMenuShown(itemListMenuEvents);
		viewChangedHandler.setOnViewChangedListener(itemListMenuEvents);

		listItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		return listItemMenuBuilder.getView(position, getItem(position), convertView, parent);
	}
}
