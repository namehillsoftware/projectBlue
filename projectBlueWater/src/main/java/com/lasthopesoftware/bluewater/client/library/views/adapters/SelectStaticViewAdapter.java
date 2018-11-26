package com.lasthopesoftware.bluewater.client.library.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.List;

public class SelectStaticViewAdapter extends ArrayAdapter<String> {

	private final SelectViewAdapterBuilder selectViewAdapterBuilder;
	private final Library.ViewType selectedViewType;
	private final int selectedViewPosition;

	public SelectStaticViewAdapter(Context context, List<String> objects, Library.ViewType selectedViewType, int selectedViewPosition) {
		super(context, R.layout.layout_select_views, objects);
		this.selectedViewType = selectedViewType;
		this.selectedViewPosition = selectedViewPosition;

		selectViewAdapterBuilder = new SelectViewAdapterBuilder(context);
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		final String item = getItem(position);
		return selectViewAdapterBuilder.getView(convertView, parent, item, selectedViewType == Library.ViewType.DownloadView && position == selectedViewPosition);
	}
}
