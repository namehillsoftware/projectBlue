package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.menu.ItemMenu;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	public ItemListAdapter(Activity activity, int resource, List<T> items) {
		super(activity, resource, items);
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		return ItemMenu.getView(getItem(position), convertView, parent);
	}
}
