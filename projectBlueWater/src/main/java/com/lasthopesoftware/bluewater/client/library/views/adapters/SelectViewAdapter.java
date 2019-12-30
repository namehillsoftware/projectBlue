package com.lasthopesoftware.bluewater.client.library.views.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.views.ServerViewItem;
import com.lasthopesoftware.bluewater.client.library.views.ViewItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SelectViewAdapter extends ArrayAdapter<ViewItem> {

	private final int selectedViewKey;
	private final SelectViewAdapterBuilder selectViewAdapterBuilder;

	public SelectViewAdapter(Context context, Collection<ViewItem> views, final int selectedViewKey) {
		super(context, R.layout.layout_select_views, views instanceof List ? (List<ViewItem>)views : new ArrayList<>(views));

		this.selectedViewKey = selectedViewKey;
		this.selectViewAdapterBuilder = new SelectViewAdapterBuilder(context);
	}
	
	@NotNull
	@Override
	public View getView(int position, View convertView, @NotNull ViewGroup parent) {
		final ViewItem item = getItem(position);
		return selectViewAdapterBuilder.getView(
			convertView,
			parent,
			item.getValue(),
			item.getKey() == selectedViewKey && item instanceof ServerViewItem);
	}
}
