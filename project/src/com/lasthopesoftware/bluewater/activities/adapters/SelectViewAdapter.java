package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;

public class SelectViewAdapter extends ArrayAdapter<IItem<?>> {

	public SelectViewAdapter(Context context, int resource, List<IItem<?>> views) {
		super(context, resource, views);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_select_views, null);
		
		TextView tvViewName = (TextView) returnView.findViewById(R.id.tvViewName);
		tvViewName.setText(getItem(position).getValue());
		
		return returnView;
	}
}
