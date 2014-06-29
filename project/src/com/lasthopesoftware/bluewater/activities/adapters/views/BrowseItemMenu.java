package com.lasthopesoftware.bluewater.activities.adapters.views;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFiles;
import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask.IOnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.IFilesContainer;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.Playlist;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class BrowseItemMenu {
	public static View getView(IItem<?> item, View convertView, ViewGroup parent) {
		final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		final ViewFlipper parentView = new ViewFlipper(parent.getContext());
		parentView.setLayoutParams(lp);
		
		final  OnSwipeListener onSwipeListener = new OnSwipeListener(parentView.getContext());
		onSwipeListener.setOnSwipeRightListener(new OnSwipeRightListener() {
			
			@Override
			public boolean onSwipeRight(View view) {
				parentView.showPrevious();
				return true;
			}
		});
		parentView.setOnTouchListener(onSwipeListener);
		        
        final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final RelativeLayout rl = (RelativeLayout)inflater.inflate(R.layout.layout_standard_text, parentView, false);
        final TextView textView = (TextView)rl.findViewById(R.id.tvStandard);
        textView.setText(item.getValue());
        parentView.addView(rl);
        
        final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, parentView, false);
        fileMenu.setOnTouchListener(onSwipeListener);
        
        final ImageButton shuffleButton = (ImageButton)fileMenu.findViewById(R.id.btnShuffle);
        shuffleButton.setOnClickListener(new ShuffleClickHandler((IFilesContainer)item));
        shuffleButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlayAll);
        playButton.setOnClickListener(new PlayClickHandler((IFilesContainer)item));
        playButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton viewButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFiles);
        viewButton.setOnClickListener(new ViewFilesClickHandler(item));
        viewButton.setOnTouchListener(onSwipeListener);
		
		parentView.addView(fileMenu);
		
		return parentView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public PlayClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			try {
				StreamingMusicService.streamMusic(v.getContext(), mItem.getJrFiles().getFileStringList());
			} catch (IOException io) {
				final View _view = v;
				PollConnectionTask.Instance.get(v.getContext()).addOnConnectionRegainedListener(new IOnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						onClick(_view);
					}
				});
				
				WaitForConnectionDialog.show(v.getContext());
			}
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public ShuffleClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			try {
				StreamingMusicService.streamMusic(v.getContext(), mItem.getJrFiles().getFileStringList(Files.GET_SHUFFLED));
			}  catch (IOException io) {
				final View _view = v;
				PollConnectionTask.Instance.get(v.getContext()).addOnConnectionRegainedListener(new IOnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						onClick(_view);
					}
				});
				
				WaitForConnectionDialog.show(v.getContext());
			} 
		}
	}
	
	private static class ViewFilesClickHandler implements OnClickListener {
		private IItem<?> mItem;
		
		public ViewFilesClickHandler(IItem<?> item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFiles.class);
    		intent.setAction(mItem instanceof Playlist ? ViewFiles.VIEW_PLAYLIST_FILES : ViewFiles.VIEW_ITEM_FILES);
    		intent.putExtra(ViewFiles.KEY, mItem.getKey());
    		v.getContext().startActivity(intent);
		}
	}
}
