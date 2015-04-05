package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.List;

public class FileListAdapter extends AbstractFileListAdapter {
	
	private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton addButton) {
			super(viewFileDetailsButton, playButton);
			
			this.addButton = addButton;
		}

		public final ImageButton addButton;
	}
	
	public FileListAdapter(Context context, int resource, List<IFile> files) {
		super(context, resource, files);
		
	}
		
	@Override
	protected final boolean getIsFilePlaying(int position, IFile file, List<IFile> nowPlayingFiles, IFile nowPlayingFile) {
		return nowPlayingFile.getKey() == file.getKey();
	}

	@Override
	protected final View getMenuView(int position, View convertView, ViewFlipper parent) {
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, parent, false);
	        final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
	        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);

	        fileMenu.setTag(new ViewHolder(viewFileDetailsButton, playButton, addButton));
	        
	        convertView = fileMenu;
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		
		final IFile file = getItem(position);
		viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(parent, file));
		viewHolder.addButton.setOnClickListener(new AddClickListener(parent, file));
		viewHolder.playButton.setOnClickListener(new FilePlayClickListener(parent, position, getFiles()));
		
		return convertView;
	}
	
	private static class AddClickListener extends AbstractMenuClickHandler {
		private IFile mFile;
		
		public AddClickListener(ViewFlipper viewFlipper, IFile file) {
            super(viewFlipper);
			mFile = file;
		}
		
		@Override
		public void onClick(final View view) {
			if (PlaybackService.getPlaylistController() != null) 
				PlaybackService.getPlaylistController().addFile(mFile);
			
			LibrarySession.GetLibrary(view.getContext(), new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					String newFileString = result.getSavedTracksString();
					if (!newFileString.endsWith(";")) newFileString += ";";
					newFileString += mFile.getKey() + ";";
					result.setSavedTracksString(newFileString);
					
					LibrarySession.SaveLibrary(view.getContext(), result, new OnCompleteListener<Void, Void, Library>() {
						
						@Override
						public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
							Toast.makeText(view.getContext(), view.getContext().getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();;
						}
					});
				}

			});

            super.onClick(view);
		}
	}

}