package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewChangedListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.List;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter {

	private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final FileListItemContainer fileListItemContainer, final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton removeButton) {
			super(viewFileDetailsButton, playButton);
			
			this.removeButton = removeButton;
            this.fileListItemContainer = fileListItemContainer;
		}

		public final ImageButton removeButton;
        public final FileListItemContainer fileListItemContainer;
        public AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public GetFileListItemTextTask getFileListItemTextTask;
	}

    private OnViewChangedListener onViewChangedListener;

    private final OnViewChangedListener onViewChangedListenerWrapper = new OnViewChangedListener() {
        @Override
        public void onViewChanged(ViewAnimator viewAnimator) {
            onViewChangedListener.onViewChanged(viewAnimator);
        }
    };

    private final int mNowPlayingFilePos;
	
	public NowPlayingFileListAdapter(Context context, int resource, List<IFile> files, int nowPlayingFilePos) {
		super(context, resource, files);

        mNowPlayingFilePos = nowPlayingFilePos;
	}

    public final View getView(final int position, View convertView, final ViewGroup parent) {
        final IFile file = getItem(position);

        if (convertView == null) {
            final FileListItemContainer fileItemMenu = new FileListItemContainer(parent.getContext());
            final NotifyOnFlipViewAnimator viewFlipper = fileItemMenu.getViewAnimator();
            convertView = viewFlipper;

            final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false);
            final ImageButton removeButton = (ImageButton)fileMenu.findViewById(R.id.btnRemoveFromPlaylist);
            final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
            final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);

            viewFlipper.addView(fileMenu);

            viewFlipper.setTag(new ViewHolder(fileItemMenu, viewFileDetailsButton, playButton, removeButton));
            viewFlipper.setViewChangedListener(onViewChangedListenerWrapper);
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextView();

        if (viewHolder.getFileListItemTextTask != null) viewHolder.getFileListItemTextTask.cancel(false);
        viewHolder.getFileListItemTextTask = new GetFileListItemTextTask(file, textView);
        viewHolder.getFileListItemTextTask.execute();

        textView.setTypeface(null, Typeface.NORMAL);

        if (position == mNowPlayingFilePos)
            textView.setTypeface(null, Typeface.BOLD);

        if (PlaybackService.getPlaylistController() != null)
            textView.setTypeface(null, position == PlaybackService.getPlaylistController().getCurrentPosition() ? Typeface.BOLD : Typeface.NORMAL);

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
                textView.setTypeface(null, position == controller.getCurrentPosition() ? Typeface.BOLD : Typeface.NORMAL);
            }
        };

        final NotifyOnFlipViewAnimator viewFlipper = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper);
        viewHolder.playButton.setOnClickListener(new FilePlayClickListener(viewFlipper, position, getFiles()));
        viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(viewFlipper, file));
        viewHolder.removeButton.setOnClickListener(new RemoveClickListener(viewFlipper, position, this));

        return viewFlipper;
    }

    public void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }

	private static class RemoveClickListener extends AbstractMenuClickHandler {
		private final int mPosition;
		private final NowPlayingFileListAdapter mAdapter;
		
		public RemoveClickListener(NotifyOnFlipViewAnimator parent, final int position, final NowPlayingFileListAdapter adapter) {
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
