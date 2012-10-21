package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.JrItem;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewFiles extends FragmentActivity {

	public static final String ARG_ALBUM_POSITION = "album_position";   
	public static final String ARG_CATEGORY_POSITION = "category_position";
	public static final String ARG_ARTIST_POSITION = "artist_position";
	
	private JrItem mAlbum;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAlbum = (JrItem) JrSession.selectedItem;
        
        FileListAdapter fileListAdapter = new FileListAdapter(this, mAlbum);
    	
    	setContentView(R.layout.activity_view_files);
    	
    	ListView fileListView = (ListView)findViewById(R.id.lvFilelist);
    	fileListView.setAdapter(fileListAdapter);
	}
	
	public class FileListAdapter extends BaseAdapter {
		private JrItem mAlbum;
		private Context mContext;
		
		public FileListAdapter(Context context, JrItem album) {
			mAlbum = album;
			mContext = context;
		}
		
		@Override
		public int getCount() {
			return mAlbum.getSubItems().size();
		}
	
		@Override
		public Object getItem(int position) {
			return mAlbum.getSubItems().get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return mAlbum.getSubItems().get(position).mKey;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			JrFile file = (JrFile) mAlbum.getSubItems().get(position);
			TextView tv = getGenericView(mContext);
			tv.setText(Integer.toString(file.getTrackNumber()) + ". " + file.mValue);
			return tv;
		}
		
		public TextView getGenericView(Context context) {
	        // Layout parameters for the ExpandableListView
	        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, 64);

	        TextView textView = new TextView(context);
	        textView.setLayoutParams(lp);
	        // Center the text vertically
	        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//	        textView.setTextColor(getResources().getColor(marcyred));
	        // Set the text starting position
	        textView.setPadding(48, 0, 0, 0);
	        return textView;
	    }
			   
	}

}
