package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FileListAdapter extends BaseAdapter {
	private ArrayList<JrFile> mFiles;
	private Context mContext; 
	
	public FileListAdapter(Context context, ArrayList<JrFile> files) {
		mFiles = files;
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
		final ListView parentListView = (ListView)parent;
		final int listPosition = position;		
		final JrFile file = mFiles.get(position);
		
		// Layout parameters for the ExpandableListView
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
        final TextView textView = new TextView(mContext);
        textView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setEllipsize(TruncateAt.END);
        textView.setSingleLine();
        textView.setMarqueeRepeatLimit(1);
        // Set the text starting position        
        textView.setPadding(20, 20, 20, 20);
        textView.setText("Loading...");
        SimpleTask<String, Void, String> getFileNameTask = new SimpleTask<String, Void, String>();
        getFileNameTask.addOnExecuteListener(new OnExecuteListener<String, Void, String>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, String> owner, String... params) throws Exception {
				if ((listPosition < parentListView.getFirstVisiblePosition() - 10) || (listPosition > parentListView.getLastVisiblePosition() + 10)) return;
				owner.setResult(file.getValue());
			}
		});
        
        getFileNameTask.addOnCompleteListener(new OnCompleteListener<String, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
				textView.setText(result);
			}
		});
        
        getFileNameTask.execute();
                
		return textView;
	}
}