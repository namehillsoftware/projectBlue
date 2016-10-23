package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.menu;

import android.content.Context;
import android.content.Intent;
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
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.FileNameTextViewSetter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.menu.listeners.RemovePlaylistFileClickListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

/**
 * Created by david on 11/7/15.
 */
public class NowPlayingFileListItemMenuBuilder extends AbstractListItemMenuBuilder<IFile> {

    private static final class ViewHolder extends BaseMenuViewHolder {

        private final LazyViewFinder<ImageButton> removeButtonFinder;

        public ViewHolder(final FileListItemContainer fileListItemContainer, final LazyViewFinder<ImageButton> viewFileDetailsButtonFinder, final LazyViewFinder<ImageButton> playButtonFinder, final LazyViewFinder<ImageButton> removeButtonFinder) {
            super(viewFileDetailsButtonFinder, playButtonFinder);

            this.removeButtonFinder = removeButtonFinder;
            this.fileListItemContainer = fileListItemContainer;
        }

        public final FileListItemContainer fileListItemContainer;
        public AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public CachedFilePropertiesProvider filePropertiesProvider;

	    public final ImageButton getRemoveButton() {
		    return removeButtonFinder.findView();
	    }
    }

    private final List<IFile> files;
    private final int nowPlayingPosition;

    private OneParameterAction<Integer> onPlaylistFileRemovedListener;

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

            viewFlipper.addView(fileMenu);

            viewFlipper.setTag(
		            new ViewHolder(
				            fileItemMenu,
				            new LazyViewFinder<>(fileMenu, R.id.btnViewFileDetails),
				            new LazyViewFinder<>(fileMenu, R.id.btnPlaySong),
				            new LazyViewFinder<>(fileMenu, R.id.btnRemoveFromPlaylist)));
            viewFlipper.setViewChangedListener(getOnViewChangedListener());
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextViewFinder();

        if (viewHolder.filePropertiesProvider != null) viewHolder.filePropertiesProvider.cancel(false);
        viewHolder.filePropertiesProvider = FileNameTextViewSetter.startNew(file, textView);

        textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == nowPlayingPosition));

        final int currentPlaylistPosition = PlaybackService.getCurrentPlaylistPosition();
        if (currentPlaylistPosition > -1)
            textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == currentPlaylistPosition));

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int playlistPosition = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaylistParameters.playlistPosition, -1);
                textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition));
            }
        };

        final NotifyOnFlipViewAnimator viewFlipper = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper);
        viewHolder.getPlayButton().setOnClickListener(new FilePlayClickListener(viewFlipper, position, files));
        viewHolder.getViewFileDetailsButton().setOnClickListener(new ViewFileDetailsClickListener(viewFlipper, file));
        viewHolder.getRemoveButton().setOnClickListener(new RemovePlaylistFileClickListener(viewFlipper, position, onPlaylistFileRemovedListener));

        return viewFlipper;
    }

    public void setOnPlaylistFileRemovedListener(OneParameterAction<Integer> onPlaylistFileRemovedListener) {
        this.onPlaylistFileRemovedListener = onPlaylistFileRemovedListener;
    }
}
