package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.BrowseLibrary;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SelectedView;

public class SelectViewAdapter extends ArrayAdapter<IJrItem<?>> {

	private List<IJrItem<?>> mViews;
	
	public SelectViewAdapter(Context context, int resource, List<IJrItem<?>> views) {
		super(context, resource, views);
		
		mViews = views;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_select_views, null);
		
		final IJrItem<?> _view = mViews.get(position);
		
		TextView tvViewName = (TextView) returnView.findViewById(R.id.tvViewName);
		tvViewName.setText(_view.getValue());
		
//		returnView.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
////				ArrayList<SelectedView> selectedViews = new ArrayList<SelectedView>();
////				SelectedView selectedView = new SelectedView();
////				selectedView.setName(_view.getValue());
////				selectedView.setServiceKey(_view.getKey());
////				selectedViews.add(selectedView);
////				JrSession.GetLibrary(v.getContext()).setSelectedViews(selectedViews);
//				
//				JrSession.JrFs = new JrFileSystem(_view.getKey());
//				
//				Intent intent = new Intent(v.getContext(), BrowseLibrary.class);
//				v.getContext().startActivity(intent);
//			}
//		});
		
		return returnView;
	}
}
