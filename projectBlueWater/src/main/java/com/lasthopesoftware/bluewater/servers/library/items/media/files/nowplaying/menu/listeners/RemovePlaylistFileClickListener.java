package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.menu.listeners;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 11/8/15.
 */
public class RemovePlaylistFileClickListener extends AbstractMenuClickHandler {
    private final int position;
    private final OneParameterRunnable<Integer> onPlaylistFileRemoved;

    // TODO Add event and remove interdepency on NowPlayingFileListAdapter adapter
    public RemovePlaylistFileClickListener(NotifyOnFlipViewAnimator parent, final int position, final OneParameterRunnable<Integer> onPlaylistFileRemoved) {
        super(parent);
        this.position = position;
        this.onPlaylistFileRemoved = onPlaylistFileRemoved;
    }

    @Override
    public void onClick(final View view) {
        PlaybackService.removeFileAtPositionFromPlaylist(view.getContext(), position);

        if (onPlaylistFileRemoved != null)
            onPlaylistFileRemoved.run(position);

        super.onClick(view);
    }
}
