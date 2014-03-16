package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

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
		
		// Layout parameters for the ExpandableListView
		final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
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
        
        GetFileValueTask.getFileValue(position, mFiles.get(position), (ListView)parent, textView);
                
		return textView;
	}
	
	private static class GetFileValueTask extends AsyncTask<String, Void, String> {
		private int mPosition;
		private ListView mParentListView;
		private TextView mTextView;
		private JrFile mFile;
		
		public static GetFileValueTask getFileValue(int position, JrFile file, ListView parentListView, TextView textView) {
			return (GetFileValueTask) (new GetFileValueTask(position, file, parentListView, textView)).execute();
		}
		
		private GetFileValueTask(int position, JrFile file, ListView parentListView, TextView textView) {
			mPosition = position;
			mParentListView = parentListView;
			mFile = file;
			mTextView = textView;
		}

		@Override
		protected String doInBackground(String... params) {
			if ((mPosition < mParentListView.getFirstVisiblePosition() - 10) || (mPosition > mParentListView.getLastVisiblePosition() + 10)) return null;
			return mFile.getValue();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result == null) return;
			
			mTextView.setText(result);
		}
	}
}