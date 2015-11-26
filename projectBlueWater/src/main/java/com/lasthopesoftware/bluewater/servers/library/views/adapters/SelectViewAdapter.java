package com.lasthopesoftware.bluewater.servers.library.views.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;

import java.util.List;

public class SelectViewAdapter extends ArrayAdapter<Item> {

	private final int selectedViewKey;

	public SelectViewAdapter(Context context, int resource, List<Item> views, final int selectedViewKey) {
		super(context, resource, views);
		
		this.selectedViewKey = selectedViewKey;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final IItem item = getItem(position);
		return SelectViewAdapterItem.getView(convertView, parent, item.getValue(), item.getKey() == selectedViewKey);
	}
}
