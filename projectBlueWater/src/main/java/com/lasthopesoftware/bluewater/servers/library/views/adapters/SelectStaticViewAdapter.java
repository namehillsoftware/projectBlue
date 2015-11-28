package com.lasthopesoftware.bluewater.servers.library.views.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.R;

import java.util.List;

/**
 * Created by david on 11/23/15.
 */
public class SelectStaticViewAdapter extends ArrayAdapter<String> {

	private final SelectViewAdapterBuilder selectViewAdapterBuilder;
	private String selectedItem = null;

	public SelectStaticViewAdapter(Context context, List<String> objects) {
		super(context, R.layout.layout_select_views, objects);

		selectViewAdapterBuilder = new SelectViewAdapterBuilder(context);
	}

	public void setSelectedItem(String selectedItem) {
		this.selectedItem = selectedItem;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final String item = getItem(position);
		return selectViewAdapterBuilder.getView(convertView, parent, item, item.equals(selectedItem));
	}
}
