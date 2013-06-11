package com.lasthopesoftware.bluewater;

import java.util.ArrayList;

import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FileListAdapter extends BaseAdapter {
	private ArrayList<JrFile> mFiles;
	private Context mContext;
	
	public FileListAdapter(Context context, IJrItem<?> item) {
		mFiles = item.getFiles();
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return mFiles.size();
	}

	@Override
	public Object getItem(int position) {
		return mFiles.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mFiles.get(position).getKey();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final JrFile file = mFiles.get(position);
		TextView tv = getGenericView(mContext);
		tv.setText(file.getValue());		
		return tv;
	}
	
	public TextView getGenericView(Context context) {
        // Layout parameters for the ExpandableListView
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView textView = new TextView(context);
        textView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//	        textView.setTextColor(getResources().getColor(marcyred));
        // Set the text starting position        
        textView.setPadding(20, 20, 20, 20);
        return textView;
    }
}