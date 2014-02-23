package com.lasthopesoftware.bluewater.activities.common;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFiles;
import com.lasthopesoftware.bluewater.activities.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrFilesContainer;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class BrowseItemMenu {
	public static View getView(IJrItem<?> item, View convertView, ViewGroup parent) {
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		final ViewFlipper parentView = new ViewFlipper(parent.getContext());
		parentView.setLayoutParams(lp);
		
		OnSwipeListener onSwipeListener = new OnSwipeListener(parentView.getContext());
		onSwipeListener.setOnSwipeRightListener(new OnSwipeRightListener() {
			
			@Override
			public boolean onSwipeRight(View view) {
				parentView.showPrevious();
				return true;
			}
		});
		parentView.setOnTouchListener(onSwipeListener);
		        
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout rl = (RelativeLayout)inflater.inflate(R.layout.layout_standard_text, null);
        TextView textView = (TextView)rl.findViewById(R.id.tvStandard);
        textView.setText(item.getValue());
        parentView.addView(rl);
        
        LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, null);
        fileMenu.setOnTouchListener(onSwipeListener);
        
        ImageButton shuffleButton = (ImageButton)fileMenu.findViewById(R.id.btnShuffle);
        shuffleButton.setOnClickListener(new ShuffleClickHandler((IJrFilesContainer)item));
        shuffleButton.setOnTouchListener(onSwipeListener);
        
        ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlayAll);
        playButton.setOnClickListener(new PlayClickHandler((IJrFilesContainer)item));
        playButton.setOnTouchListener(onSwipeListener);
        
        ImageButton viewButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFiles);
        viewButton.setOnClickListener(new ViewFilesClickHandler(item));
        viewButton.setOnTouchListener(onSwipeListener);
		
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
			try {
				StreamingMusicService.StreamMusic(v.getContext(), mItem.getJrFiles().getFileStringList());
			} catch (IOException io) {
				final View _view = v;
				PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
					
					@Override
					public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
						if (result)
							onClick(_view);
					}
				});
				
				WaitForConnectionDialog.show(v.getContext());
			}
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private IJrFilesContainer mItem;
		
		public ShuffleClickHandler(IJrFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			try {
				StreamingMusicService.StreamMusic(v.getContext(), mItem.getJrFiles().getFileStringList(JrFiles.GET_SHUFFLED));
			}  catch (IOException io) {
				final View _view = v;
				PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
					
					@Override
					public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
						if (result)
							onClick(_view);
					}
				});
				
				WaitForConnectionDialog.show(v.getContext());
			} 
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
    		v.getContext().startActivity(intent);
		}
	}
	
	public static class LongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			for (int i = 0; i < parent.getChildCount(); i++) {
				View child = parent.getChildAt(i);
				if (child instanceof ViewFlipper) {
					ViewFlipper flipper = ((ViewFlipper)child); 
					if (flipper.getDisplayedChild() == 0) continue;
					
					flipper.showPrevious();
				}
			}
			if (view instanceof ViewFlipper) {
				ViewFlipper parentView = (ViewFlipper)view;
				parentView.showNext();
				return true;
			}
			return false;
		}
	}
}
