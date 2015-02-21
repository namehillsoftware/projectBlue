package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter {

	private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton removeButton) {
			super(viewFileDetailsButton, playButton);
			
			this.removeButton = removeButton;
		}

		public final ImageButton removeButton;
	}
	
	public NowPlayingFileListAdapter(Context context, int resource, List<IFile> files) {
		super(context, resource, files);
		
	}

	@Override
	protected boolean getIsFilePlaying(int position, IFile file, List<IFile> nowPlayingFiles, IFile nowPlayingFile) {
		return position == nowPlayingFiles.indexOf(nowPlayingFile);
	}

	@Override
	protected View getMenuView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false);
//	        fileMenu.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton removeButton = (ImageButton)fileMenu.findViewById(R.id.btnRemoveFromPlaylist);
//	        addButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
//	        playButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);
//	        viewFileDetailsButton.setOnTouchListener(onSwipeListener);
	        
	        fileMenu.setTag(new ViewHolder(viewFileDetailsButton, playButton, removeButton));
	        
	        convertView = fileMenu;
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		
		final IFile file = getItem(position);
		viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(file));
		viewHolder.removeButton.setOnClickListener(new RemoveClickListener(position, this));
		viewHolder.playButton.setOnClickListener(new FilePlayClickListener(position, getFiles()));
		
		return convertView;
	}
	
	private static class RemoveClickListener implements OnClickListener {
		private final int mPosition;
		private final NowPlayingFileListAdapter mAdapter;
		
		public RemoveClickListener(final int position, final NowPlayingFileListAdapter adapter) {
			mPosition = position;
			mAdapter = adapter;
		}
		
		@Override
		public void onClick(final View view) {
			LibrarySession.GetLibrary(view.getContext(), new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					
					String newFileString = PlaybackService.getPlaylistController().getPlaylistString();					
					result.setSavedTracksString(newFileString);
					
					LibrarySession.SaveLibrary(view.getContext(), result, new OnCompleteListener<Void, Void, Library>() {
						
						@Override
						public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
							if (PlaybackService.getPlaylistController() != null) 
								PlaybackService.getPlaylistController().removeFile(mPosition);
							
							mAdapter.remove(mAdapter.getItem(mPosition));
						}
					});
				}

			});
		}
	}
}
