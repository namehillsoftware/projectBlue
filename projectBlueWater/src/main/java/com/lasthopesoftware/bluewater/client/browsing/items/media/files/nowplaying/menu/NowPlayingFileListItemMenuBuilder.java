package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.FileSeekToClickListener;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.RemovePlaylistFileClickListener;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.AbstractListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

public class NowPlayingFileListItemMenuBuilder extends AbstractListItemMenuBuilder<ServiceFile> {

    private static final class ViewHolder extends BaseMenuViewHolder {

        final FileNameTextViewSetter fileNameTextViewSetter;
        private final LazyViewFinder<ImageButton> removeButtonFinder;

        final FileListItemContainer fileListItemContainer;
        AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public Promise<?> filePropertiesProvider;

        ViewHolder(final FileListItemContainer fileListItemContainer, FileNameTextViewSetter fileNameTextViewSetter, final LazyViewFinder<ImageButton> viewFileDetailsButtonFinder, final LazyViewFinder<ImageButton> playButtonFinder, final LazyViewFinder<ImageButton> removeButtonFinder) {
            super(viewFileDetailsButtonFinder, playButtonFinder);
            this.fileNameTextViewSetter = fileNameTextViewSetter;

            this.removeButtonFinder = removeButtonFinder;
            this.fileListItemContainer = fileListItemContainer;
        }

	    final ImageButton getRemoveButton() {
		    return removeButtonFinder.findView();
	    }
    }

    private final INowPlayingRepository nowPlayingRepository;

    private OneParameterAction<Integer> onPlaylistFileRemovedListener;

    public NowPlayingFileListItemMenuBuilder(final INowPlayingRepository nowPlayingRepository) {
        this.nowPlayingRepository = nowPlayingRepository;
    }

    @Override
    public View getView(final int position, final ServiceFile serviceFile, View convertView, ViewGroup parent) {
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
                            new FileNameTextViewSetter(fileItemMenu.findTextView()),
				            new LazyViewFinder<>(fileMenu, R.id.btnViewFileDetails),
				            new LazyViewFinder<>(fileMenu, R.id.btnPlaySong),
				            new LazyViewFinder<>(fileMenu, R.id.btnRemoveFromPlaylist)));
            viewFlipper.setViewChangedListener(getOnViewChangedListener());
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.findTextView();

        viewHolder.filePropertiesProvider = viewHolder.fileNameTextViewSetter.promiseTextViewUpdate(serviceFile);

		nowPlayingRepository
			.getNowPlaying()
			.eventually(LoopedInPromise.response(np -> {
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np.playlistPosition));
				return null;
			}, textView.getContext()));

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1);
                textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition));
            }
        };

        final NotifyOnFlipViewAnimator viewFlipper = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper);
        viewHolder.getPlayButton().setOnClickListener(new FileSeekToClickListener(viewFlipper, position));
        viewHolder.getViewFileDetailsButton().setOnClickListener(new ViewFileDetailsClickListener(viewFlipper, serviceFile));
        viewHolder.getRemoveButton().setOnClickListener(new RemovePlaylistFileClickListener(viewFlipper, position, onPlaylistFileRemovedListener));

        return viewFlipper;
    }

    public void setOnPlaylistFileRemovedListener(OneParameterAction<Integer> onPlaylistFileRemovedListener) {
        this.onPlaylistFileRemovedListener = onPlaylistFileRemovedListener;
    }
}
