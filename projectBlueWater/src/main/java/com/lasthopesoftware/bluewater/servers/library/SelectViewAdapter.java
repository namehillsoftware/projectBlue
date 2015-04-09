package com.lasthopesoftware.bluewater.servers.library;

import android.content.Context;
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

	private final int mSelectedViewKey;
	private static int mSelectedColor = -1;
	
	public SelectViewAdapter(Context context, int resource, List<Item> views, final int selectedViewKey) {
		super(context, resource, views);
		
		mSelectedViewKey = selectedViewKey;
		
		if (mSelectedColor == -1)
			mSelectedColor = context.getResources().getColor(R.color.clearstream_blue);
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
		
		if (item.getKey() == mSelectedViewKey)
			tvViewName.setBackgroundColor(mSelectedColor);
		
		return convertView;
	}
}
