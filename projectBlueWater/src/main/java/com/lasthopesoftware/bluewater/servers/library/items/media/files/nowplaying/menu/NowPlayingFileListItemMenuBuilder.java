package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.menu;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.menu.listeners.RemovePlaylistFileClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.vedsoft.futures.runnables.OneParameterRunnable;

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

    private OneParameterRunnable<Integer> onPlaylistFileRemovedListener;

    public NowPlayingFileListItemMenuBuilder(final List<IFile> files, final int nowPlayingPosition) {
        this.files = files;
        this.nowPlayingPosition = nowPlayingPosition;
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

        textView.setTypeface(null, position == PlaybackService.getCurrentPlaylistPosition() ? Typeface.BOLD : Typeface.NORMAL);

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int playlistPosition = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaylistParameters.playlistPosition, -1);
                textView.setTypeface(null, position == playlistPosition ? Typeface.BOLD : Typeface.NORMAL);
            }
        };

        final NotifyOnFlipViewAnimator viewFlipper = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper);
        viewHolder.playButton.setOnClickListener(new FilePlayClickListener(viewFlipper, position, files));
        viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(viewFlipper, file));
        viewHolder.removeButton.setOnClickListener(new RemovePlaylistFileClickListener(viewFlipper, position, onPlaylistFileRemovedListener));

        return viewFlipper;
    }

    public void setOnPlaylistFileRemovedListener(OneParameterRunnable<Integer> onPlaylistFileRemovedListener) {
        this.onPlaylistFileRemovedListener = onPlaylistFileRemovedListener;
    }
}
