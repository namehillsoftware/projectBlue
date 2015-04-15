package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;

/**
 * Created by david on 4/14/15.
 */
public abstract class AbstractFileListItemNowPlayingHandler implements OnNowPlayingStartListener {

    private final FileListItemContainer mFileListItem;

    private View.OnAttachStateChangeListener onAttachStateChangeListener;

    public AbstractFileListItemNowPlayingHandler(FileListItemContainer fileListItem) {
        mFileListItem = fileListItem;

        final OnNowPlayingStartListener onNowPlayingStartListener = this;

        PlaybackService.addOnStreamingStartListener(onNowPlayingStartListener);

        onAttachStateChangeListener = new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View v) {
                PlaybackService.removeOnStreamingStartListener(onNowPlayingStartListener);
            }

            @Override
            public void onViewAttachedToWindow(View v) { }
        };

        mFileListItem.getTextViewContainer().addOnAttachStateChangeListener(onAttachStateChangeListener);
    }

    public void release() {
        mFileListItem.getTextViewContainer().removeOnAttachStateChangeListener(onAttachStateChangeListener);
    }
}
