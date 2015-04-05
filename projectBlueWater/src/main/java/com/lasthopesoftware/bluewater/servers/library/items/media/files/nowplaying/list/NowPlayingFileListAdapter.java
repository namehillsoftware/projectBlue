package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.List;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter {

	private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton removeButton) {
			super(viewFileDetailsButton, playButton);
			
			this.removeButton = removeButton;
		}

		public final ImageButton removeButton;
	}

    private final int mNowPlayingFilePos;
	
	public NowPlayingFileListAdapter(Context context, int resource, List<IFile> files, int nowPlayingFilePos) {
		super(context, resource, files);

        mNowPlayingFilePos = nowPlayingFilePos;
	}

	@Override
	protected boolean getIsFilePlaying(int position, IFile file, List<IFile> nowPlayingFiles, IFile nowPlayingFile) {
		return position == nowPlayingFiles.indexOf(nowPlayingFile);
	}

    @Override
    protected void onTextViewPopulated(int position, IFile file, TextView textView) {
        if (position == mNowPlayingFilePos)
            textView.setTypeface(null, Typeface.BOLD);
    }

    @Override
	protected View getMenuView(int position, View convertView, ViewFlipper parent) {
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false);
	        final ImageButton removeButton = (ImageButton)fileMenu.findViewById(R.id.btnRemoveFromPlaylist);
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
	        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);

	        fileMenu.setTag(new ViewHolder(viewFileDetailsButton, playButton, removeButton));
	        
	        convertView = fileMenu;
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		
		final IFile file = getItem(position);
		viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(parent, file));
		viewHolder.removeButton.setOnClickListener(new RemoveClickListener(parent, position, this));
		viewHolder.playButton.setOnClickListener(new FilePlayClickListener(parent, position, getFiles()));
		
		return convertView;
	}
	
	private static class RemoveClickListener extends AbstractMenuClickHandler {
		private final int mPosition;
		private final NowPlayingFileListAdapter mAdapter;
		
		public RemoveClickListener(ViewFlipper parent, final int position, final NowPlayingFileListAdapter adapter) {
            super(parent);
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

            super.onClick(view);
		}
	}
}
