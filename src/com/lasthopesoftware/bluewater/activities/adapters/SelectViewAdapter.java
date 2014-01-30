package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SelectedView;

public class SelectViewAdapter extends ArrayAdapter<IJrItem<?>> {

	private List<IJrItem<?>> mViews;
	private Collection<SelectedView> mSelectedViews;
	
	public SelectViewAdapter(Context context, int resource, List<IJrItem<?>> views, Collection<SelectedView> selectedViews) {
		super(context, resource, views);
		
		mViews = views;
		mSelectedViews = selectedViews;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_select_views, null);
		
		final IJrItem<?> _view = mViews.get(position);
		
		TextView tvViewName = (TextView) returnView.findViewById(R.id.tvViewName);
		tvViewName.setText(_view.getValue());
		
		CheckBox cbView = (CheckBox) returnView.findViewById(R.id.cbView);
		cbView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				//TODO Set up view changing code here
			}
		});
		
		cbView.setChecked(false);
		for (SelectedView selectedView : mSelectedViews) {
			if (selectedView.getServiceKey() != _view.getKey().intValue()) continue;
			
			cbView.setChecked(true);
			break;
		}
		
		return returnView;
	}
}
