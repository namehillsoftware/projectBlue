package com.lasthopesoftware.bluewater.servers.library.items.files.details;

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
	
	private static class ViewHolder {
		TextView fileDetailName;
		TextView fileDetailValue;
	}
	
	public FileDetailsAdapter(Context context, int resource, List<Entry<String, String>> objects) {
		super(context, resource, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		if (convertView == null) {
			final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final LinearLayout returnView = (LinearLayout) inflator.inflate(R.layout.layout_file_details, parent, false);
			
			ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.fileDetailName = (TextView) returnView.findViewById(R.id.tvFileDetailName);
			viewHolder.fileDetailValue = (TextView) returnView.findViewById(R.id.tvFileDetailValue);
			returnView.setTag(viewHolder);
			
			convertView = returnView;
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		
		final Entry<String, String> fileProperty = getItem(position);
		viewHolder.fileDetailName.setText(fileProperty.getKey());
		viewHolder.fileDetailValue.setText(fileProperty.getValue());
        
		return convertView;
	}
}