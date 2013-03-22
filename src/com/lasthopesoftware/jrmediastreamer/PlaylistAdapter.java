package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class PlaylistAdapter extends BaseAdapter {
	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public PlaylistAdapter(Context context, ArrayList<JrPlaylist> playlists) {
		mPlaylists = playlists;
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return mPlaylists.size();
	}

	@Override
	public Object getItem(int position) {
		return mPlaylists.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mPlaylists.get(position).getKey();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		JrPlaylist playlist = mPlaylists.get(position);
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		ViewFlipper parentView = new ViewFlipper(parent.getContext());
		parentView.setLayoutParams(lp);
		
        TextView textView = new TextView(parentView.getContext());
        textView.setTextAppearance(parentView.getContext(), android.R.style.TextAppearance_Large);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//	        textView.setTextColor(getResources().getColor(marcyred));
        // Set the text starting position        
        textView.setPadding(20, 20, 20, 20);        
        textView.setText(playlist.getValue());
        
        parentView.addView(textView);
        
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.active_jr_item_menu, null);
        
        Button shuffleButton = (Button)fileMenu.findViewById(R.id.btnShuffle);
        shuffleButton.setOnClickListener(new ShuffleClickHandler(playlist));
        
        Button playButton = (Button)fileMenu.findViewById(R.id.btnPlayAll);
        playButton.setOnClickListener(new PlayClickHandler(playlist));
        
        Button viewButton = (Button)fileMenu.findViewById(R.id.btnViewFiles);
        viewButton.setOnClickListener(new ViewFilesClickHandler(playlist));
		
		parentView.addView(fileMenu);
		
		return parentView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private JrPlaylist mPlaylist;
		
		public PlayClickHandler(JrPlaylist playlist) {
			mPlaylist = playlist;
		}
		
		@Override
		public void onClick(View v) {
			JrSession.playlist = mPlaylist.getFiles();
			JrFile file = mPlaylist.getFiles().get(0);
			Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY, Uri.parse(file.getUrl()), v.getContext(), StreamingMusicService.class);
			v.getContext().startService(svcIntent);
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private JrPlaylist mPlaylist;
		
		public ShuffleClickHandler(JrPlaylist playlist) {
			mPlaylist = playlist;
		}
		
		@Override
		public void onClick(View v) {
			JrSession.playlist = mPlaylist.getFiles(JrPlaylist.GET_SHUFFLED);
			JrFile file = mPlaylist.getFiles().get(0);
			Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY, Uri.parse(file.getUrl()), v.getContext(), StreamingMusicService.class);
			v.getContext().startService(svcIntent);
		}
	}
	
	private static class ViewFilesClickHandler implements OnClickListener {
		private JrPlaylist mPlaylist;
		
		public ViewFilesClickHandler(JrPlaylist playlist) {
			mPlaylist = playlist;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFiles.class);
    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
    		intent.putExtra(ViewFiles.KEY, mPlaylist.getKey());
    		JrSession.selectedItem = mPlaylist;
    		v.getContext().startActivity(intent);
		}
	}
}
