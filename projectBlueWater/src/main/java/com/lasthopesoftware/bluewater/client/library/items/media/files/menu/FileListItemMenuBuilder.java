package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

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
import com.lasthopesoftware.bluewater.client.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.client.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.INowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

import java.util.List;

import static com.vedsoft.futures.callables.VoidFunc.runningCarelessly;

/**
 * Created by david on 11/7/15.
 */
public class FileListItemMenuBuilder extends AbstractListItemMenuBuilder<IFile> {

    private final List<IFile> files;
    private final INowPlayingFileProvider nowPlayingFileProvider;

    private static final class ViewHolder extends BaseMenuViewHolder {
        final LazyViewFinder<ImageButton> addButtonFinder;

        ViewHolder(final FileListItemContainer fileListItemContainer, final LazyViewFinder<ImageButton> viewFileDetailsButtonFinder, final LazyViewFinder<ImageButton> playButtonFinder, final LazyViewFinder<ImageButton> addButtonFinder) {
            super(viewFileDetailsButtonFinder, playButtonFinder);

            this.addButtonFinder = addButtonFinder;
            this.fileListItemContainer = fileListItemContainer;
        }

        final FileListItemContainer fileListItemContainer;
        AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public CachedFilePropertiesProvider filePropertiesProvider;

        final ImageButton getAddButton() {
            return addButtonFinder.findView();
        }
    }

    public FileListItemMenuBuilder(final List<IFile> files, INowPlayingFileProvider nowPlayingFileProvider) {
        this.files = files;
        this.nowPlayingFileProvider = nowPlayingFileProvider;
    }

    @Override
    public View getView(int position, final IFile file, View convertView, ViewGroup parent) {
        if (convertView == null) {
            final FileListItemContainer fileItemMenu = new FileListItemContainer(parent.getContext());
            final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator = fileItemMenu.getViewAnimator();
            convertView = notifyOnFlipViewAnimator;

            final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, parent, false);

            notifyOnFlipViewAnimator.addView(fileMenu);

            notifyOnFlipViewAnimator.setTag(
                    new ViewHolder(
                            fileItemMenu,
                            new LazyViewFinder<>(fileMenu, R.id.btnViewFileDetails),
                            new LazyViewFinder<>(fileMenu, R.id.btnPlaySong),
                            new LazyViewFinder<>(fileMenu, R.id.btnAddToPlaylist)));

            notifyOnFlipViewAnimator.setViewChangedListener(getOnViewChangedListener());
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextViewFinder();

        if (viewHolder.filePropertiesProvider != null) viewHolder.filePropertiesProvider.cancel(false);
        viewHolder.filePropertiesProvider = FileNameTextViewSetter.startNew(file, textView);

        textView.setTypeface(null, Typeface.NORMAL);
        nowPlayingFileProvider
            .getNowPlayingFile()
            .then(runningCarelessly(f ->
                textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(file.getKey() == f.getKey()))));

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
                textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(file.getKey() == fileKey));
            }
        };

        final NotifyOnFlipViewAnimator viewAnimator = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);
        viewHolder.getPlayButton().setOnClickListener(new FilePlayClickListener(viewAnimator, position, files));
        viewHolder.getViewFileDetailsButton().setOnClickListener(new ViewFileDetailsClickListener(viewAnimator, file));
        viewHolder.getAddButton().setOnClickListener(new AddClickListener(viewAnimator, file));

        return viewAnimator;
    }

    private static class AddClickListener extends AbstractMenuClickHandler {
        private final IFile mFile;

        AddClickListener(NotifyOnFlipViewAnimator viewFlipper, IFile file) {
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
