package com.lasthopesoftware.bluewater.servers.library.items;

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
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.IFilesContainer;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.Playlist;
import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.library.items.files.list.FileListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.NowPlayingService;
import com.lasthopesoftware.bluewater.shared.listener.OnSwipeListener;
import com.lasthopesoftware.bluewater.shared.listener.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.threading.ISimpleTask;

public class ItemMenu {
	private static class ViewHolder {
		public ViewHolder(TextView textView, ImageButton shuffleButton, ImageButton playButton, ImageButton viewButton) {
			this.textView = textView;
			this.shuffleButton = shuffleButton;
			this.playButton = playButton;
			this.viewButton = viewButton;
		}
		
		final TextView textView;
		final ImageButton shuffleButton;
		final ImageButton playButton;
		final ImageButton viewButton;
	}
	
	public static View getView(IItem<?> item, View convertView, ViewGroup parent) {
		if (convertView == null) {
		
			final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			
			convertView = new ViewFlipper(parent.getContext());
			final ViewFlipper parentView = (ViewFlipper) convertView;
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
	        
	        parentView.addView(rl);
	        
	        final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, parentView, false);
	        fileMenu.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton shuffleButton = (ImageButton)fileMenu.findViewById(R.id.btnShuffle);	        
	        shuffleButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlayAll);	        
	        playButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton viewButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFiles);
	        viewButton.setOnTouchListener(onSwipeListener);
			
			parentView.addView(fileMenu);
			
			convertView.setTag(new ViewHolder(textView, shuffleButton, playButton, viewButton));
		}
		
		if (((ViewFlipper)convertView).getDisplayedChild() != 0) ((ViewFlipper)convertView).showPrevious();
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		viewHolder.textView.setText(item.getValue());
		viewHolder.shuffleButton.setOnClickListener(new ShuffleClickHandler((IFilesContainer)item));
		viewHolder.playButton.setOnClickListener(new PlayClickHandler((IFilesContainer)item));
		viewHolder.viewButton.setOnClickListener(new ViewFilesClickHandler(item));
		
		return convertView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public PlayClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(final View v) {
			mItem.getFiles().getFileStringList(new OnGetFileStringListCompleteListener(v.getContext()), new OnGetFileStringListErrorListener(v, this));
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public ShuffleClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			mItem.getFiles().getFileStringList(Files.GET_SHUFFLED, new OnGetFileStringListCompleteListener(v.getContext()), new OnGetFileStringListErrorListener(v, this));
		}
	}
	
	private static class ViewFilesClickHandler implements OnClickListener {
		private IItem<?> mItem;
		
		public ViewFilesClickHandler(IItem<?> item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), FileListActivity.class);
    		intent.setAction(mItem instanceof Playlist ? FileListActivity.VIEW_PLAYLIST_FILES : FileListActivity.VIEW_ITEM_FILES);
    		intent.putExtra(FileListActivity.KEY, mItem.getKey());
    		v.getContext().startActivity(intent);
		}
	}
	
	private static class OnGetFileStringListCompleteListener implements OnCompleteListener<String> {
		private final Context mContext;
		
		public OnGetFileStringListCompleteListener(final Context context) {
			mContext = context;
		}
		
		@Override
		public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
			NowPlayingService.launchMusicService(mContext, result);
		}
		
	}
	
	private static class OnGetFileStringListErrorListener implements OnErrorListener<String> {
		private final View mView;
		private final OnClickListener mOnClickListener;
		
		public OnGetFileStringListErrorListener(final View view, final OnClickListener onClickListener) {
			mView = view;
			mOnClickListener = onClickListener;
		}		
		
		@Override
		public boolean onError(ISimpleTask<String, Void, String> owner, boolean isHandled, Exception innerException) {
			if (innerException instanceof IOException) {
				PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						mOnClickListener.onClick(mView);
					}
				});
				
				WaitForConnectionDialog.show(mView.getContext());
				return true;
			}
			return false;
		}
		
	}
}
