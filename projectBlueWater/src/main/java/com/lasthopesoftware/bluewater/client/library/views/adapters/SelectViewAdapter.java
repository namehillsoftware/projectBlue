package com.lasthopesoftware.bluewater.client.library.views.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.List;

public class SelectViewAdapter extends ArrayAdapter<Item> {

	private final int selectedViewKey;
	private final Library.ViewType selectedViewType;
	private final SelectViewAdapterBuilder selectViewAdapterBuilder;

	public SelectViewAdapter(Context context, List<Item> views, Library.ViewType selectedViewType, final int selectedViewKey) {
		super(context, R.layout.layout_select_views, views);

		this.selectedViewType = selectedViewType;
		this.selectedViewKey = selectedViewKey;
		this.selectViewAdapterBuilder = new SelectViewAdapterBuilder(context);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final IItem item = getItem(position);
		return selectViewAdapterBuilder.getView(convertView, parent, item.getValue(), item.getKey() == selectedViewKey && Library.ViewType.serverViewTypes.contains(selectedViewType));
	}
}
