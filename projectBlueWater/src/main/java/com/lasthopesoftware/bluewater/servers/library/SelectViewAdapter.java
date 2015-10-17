package com.lasthopesoftware.bluewater.servers.library;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;

import java.util.List;

public class SelectViewAdapter extends ArrayAdapter<Item> {

	private final int selectedViewKey;
	private static int selectedColor = -1;

	public SelectViewAdapter(Context context, int resource, List<Item> views, final int selectedViewKey) {
		super(context, resource, views);
		
		this.selectedViewKey = selectedViewKey;

		final int colorResource = R.color.clearstream_blue;
		if (selectedColor == -1)
			selectedColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? context.getColor(colorResource) : context.getResources().getColor(colorResource);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflator.inflate(R.layout.layout_select_views, parent, false);
		}
		
		final IItem item = getItem(position);
		
		final TextView tvViewName = (TextView) convertView.findViewById(R.id.tvViewName);
		tvViewName.setText(item.getValue());
		
		if (item.getKey() == selectedViewKey)
			tvViewName.setBackgroundColor(selectedColor);
		
		return convertView;
	}
}
