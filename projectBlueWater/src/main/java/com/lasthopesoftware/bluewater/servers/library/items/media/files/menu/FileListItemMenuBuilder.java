package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
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
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

import java.util.List;

/**
 * Created by david on 11/7/15.
 */
public class FileListItemMenuBuilder extends AbstractListItemMenuBuilder<IFile> {

    private final List<IFile> files;

    private static class ViewHolder extends BaseMenuViewHolder {

        public ViewHolder(final FileListItemContainer fileListItemContainer, final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton addButton) {
            super(viewFileDetailsButton, playButton);

            this.addButton = addButton;
            this.fileListItemContainer = fileListItemContainer;
        }

        public final ImageButton addButton;
        public final FileListItemContainer fileListItemContainer;
        public AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public GetFileListItemTextTask getFileListItemTextTask;
    }

    public FileListItemMenuBuilder(final List<IFile> files) {
        this.files = files;
    }

    @Override
    public View getView(int position, final IFile file, View convertView, ViewGroup parent) {
        if (convertView == null) {
            final FileListItemContainer fileItemMenu = new FileListItemContainer(parent.getContext());
            final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator = fileItemMenu.getViewAnimator();
            convertView = notifyOnFlipViewAnimator;

            final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, parent, false);
            final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
            final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
            final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);

            notifyOnFlipViewAnimator.addView(fileMenu);

            notifyOnFlipViewAnimator.setTag(new ViewHolder(fileItemMenu, viewFileDetailsButton, playButton, addButton));

            notifyOnFlipViewAnimator.setViewChangedListener(getOnViewChangedListener());
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextView();

        if (viewHolder.getFileListItemTextTask != null) viewHolder.getFileListItemTextTask.cancel(false);
        viewHolder.getFileListItemTextTask = new GetFileListItemTextTask(file, textView);
        viewHolder.getFileListItemTextTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        textView.setTypeface(null, Typeface.NORMAL);
		textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(file.getKey() == PlaybackService.getCurrentPlayingFileKey()));

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int fileKey = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileKey, -1);
                textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(file.getKey() == fileKey));
            }
        };

        final NotifyOnFlipViewAnimator viewAnimator = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);
        viewHolder.playButton.setOnClickListener(new FilePlayClickListener(viewAnimator, position, files));
        viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(viewAnimator, file));
        viewHolder.addButton.setOnClickListener(new AddClickListener(viewAnimator, file));

        return viewAnimator;
    }

    private static class AddClickListener extends AbstractMenuClickHandler {
        private final IFile mFile;

        public AddClickListener(NotifyOnFlipViewAnimator viewFlipper, IFile file) {
            super(viewFlipper);
            mFile = file;
        }

        @Override
        public void onClick(final View view) {
	        PlaybackService.addFileToPlaylist(view.getContext(), mFile.getKey());

            super.onClick(view);
        }
    }
}
