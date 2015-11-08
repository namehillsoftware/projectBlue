package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.menu;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list.NowPlayingFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

/**
 * Created by david on 11/7/15.
 */
public class NowPlayingFileListItemMenuBuilder extends AbstractListItemMenuBuilder<IFile> {

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

    private final List<IFile> files;
    private final int nowPlayingPosition;
    private final NowPlayingFileListAdapter nowPlayingFileListAdapter;

    public NowPlayingFileListItemMenuBuilder(final NowPlayingFileListAdapter nowPlayingFileListAdapter, final List<IFile> files, final int nowPlayingPosition) {
        this.files = files;
        this.nowPlayingPosition = nowPlayingPosition;
        this.nowPlayingFileListAdapter = nowPlayingFileListAdapter;
    }

    @Override
    public View getView(final int position, final IFile file, View convertView, ViewGroup parent) {
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
            viewFlipper.setViewChangedListener(getOnViewChangedListener());
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextView();

        if (viewHolder.getFileListItemTextTask != null) viewHolder.getFileListItemTextTask.cancel(false);
        viewHolder.getFileListItemTextTask = new GetFileListItemTextTask(file, textView);
        viewHolder.getFileListItemTextTask.execute();

        textView.setTypeface(null, Typeface.NORMAL);

        if (position == nowPlayingPosition)
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
        viewHolder.playButton.setOnClickListener(new FilePlayClickListener(viewFlipper, position, files));
        viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(viewFlipper, file));
        viewHolder.removeButton.setOnClickListener(new RemoveClickListener(viewFlipper, position, nowPlayingFileListAdapter));

        return viewFlipper;
    }

    private static class RemoveClickListener extends AbstractMenuClickHandler {
        private final int position;
        private final NowPlayingFileListAdapter adapter;

        // TODO Add event and remove interdepency on NowPlayingFileListAdapter adapter
        public RemoveClickListener(NotifyOnFlipViewAnimator parent, final int position, final NowPlayingFileListAdapter adapter) {
            super(parent);
            this.position = position;
            this.adapter = adapter;
        }

        @Override
        public void onClick(final View view) {
            LibrarySession.GetLibrary(view.getContext(), new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

                @Override
                public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
                    if (result == null) return;

                    String newFileString = PlaybackService.getPlaylistController().getPlaylistString();
                    result.setSavedTracksString(newFileString);

                    LibrarySession.SaveLibrary(view.getContext(), result, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {

                        @Override
                        public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
                            if (PlaybackService.getPlaylistController() != null)
                                PlaybackService.getPlaylistController().removeFile(position);

                            adapter.remove(adapter.getItem(position));
                        }
                    });
                }

            });

            super.onClick(view);
        }
    }
}
