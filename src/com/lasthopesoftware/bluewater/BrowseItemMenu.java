package com.lasthopesoftware.bluewater;

import java.util.ArrayList;

import com.lasthopesoftware.bluewater.R;
import jrFileSystem.IJrItemFiles;
import jrAccess.JrSession;
import jrFileSystem.IJrFilesContainer;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class BrowseItemMenu {
	public static View getView(IJrItem<?> item, View convertView, ViewGroup parent) {
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
        textView.setText(item.getValue());
        
        parentView.addView(textView);
        
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.active_jr_item_menu, null);
        
        Button shuffleButton = (Button)fileMenu.findViewById(R.id.btnShuffle);
        shuffleButton.setOnClickListener(new ShuffleClickHandler((IJrFilesContainer)item));
        
        Button playButton = (Button)fileMenu.findViewById(R.id.btnPlayAll);
        playButton.setOnClickListener(new PlayClickHandler((IJrFilesContainer)item));
        
        Button viewButton = (Button)fileMenu.findViewById(R.id.btnViewFiles);
        viewButton.setOnClickListener(new ViewFilesClickHandler(item));
		
		parentView.addView(fileMenu);
		
		return parentView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private IJrFilesContainer mItem;
		
		public PlayClickHandler(IJrFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			ArrayList<JrFile> playlist = mItem.getJrFiles().getFiles();
			StreamingMusicService.StreamMusic(v.getContext(), playlist.get(0), playlist);
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private IJrFilesContainer mItem;
		
		public ShuffleClickHandler(IJrFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			ArrayList<JrFile> playlist = mItem.getJrFiles().getFiles(JrPlaylist.GET_SHUFFLED);
			StreamingMusicService.StreamMusic(v.getContext(), playlist.get(0), playlist);
		}
	}
	
	private static class ViewFilesClickHandler implements OnClickListener {
		private IJrItem<?> mItem;
		
		public ViewFilesClickHandler(IJrItem<?> item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFiles.class);
    		intent.setAction(mItem instanceof JrPlaylist ? ViewFiles.VIEW_PLAYLIST_FILES : ViewFiles.VIEW_ITEM_FILES);
    		intent.putExtra(ViewFiles.KEY, mItem.getKey());
    		JrSession.selectedItem = mItem;
    		v.getContext().startActivity(intent);
		}
	}
	
	public static class ClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			if (view instanceof ViewFlipper) {
				ViewFlipper parentView = (ViewFlipper)view;
				parentView.showNext();
				return true;
			}
			return false;
		}
	}
}
