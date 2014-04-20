package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;

public class FileDetailsAdapter extends ArrayAdapter<Entry<String, String>> {
	
	public FileDetailsAdapter(Context context, int resource, List<Entry<String, String>> objects) {
		super(context, resource, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final Entry<String, String> fileProperty = getItem(position);
		final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final LinearLayout returnView = (LinearLayout) inflator.inflate(R.layout.layout_file_details, null);
		((TextView) returnView.findViewById(R.id.tvFileDetailName)).setText(fileProperty.getKey());;
		((TextView) returnView.findViewById(R.id.tvFileDetailValue)).setText(fileProperty.getValue());;
        
		return returnView;
	}
}