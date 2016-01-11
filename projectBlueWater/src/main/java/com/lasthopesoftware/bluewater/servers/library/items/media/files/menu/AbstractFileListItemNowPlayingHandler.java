package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;

/**
 * Created by david on 4/14/15.
 */
public abstract class AbstractFileListItemNowPlayingHandler extends BroadcastReceiver {

    private final RelativeLayout fileTextViewContainer;

    private View.OnAttachStateChangeListener onAttachStateChangeListener;

    private final LocalBroadcastManager localBroadcastManager;

    public AbstractFileListItemNowPlayingHandler(FileListItemContainer fileListItem) {
        fileTextViewContainer = fileListItem.getTextViewContainer();

        localBroadcastManager = LocalBroadcastManager.getInstance(fileTextViewContainer.getContext());

        localBroadcastManager.registerReceiver(this, new IntentFilter(PlaybackService.PlaylistEvents.onPlaylistStart));

        onAttachStateChangeListener = new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View v) {
                localBroadcastManager.unregisterReceiver(AbstractFileListItemNowPlayingHandler.this);
            }

            @Override
            public void onViewAttachedToWindow(View v) { }
        };

        fileTextViewContainer.addOnAttachStateChangeListener(onAttachStateChangeListener);
    }

    public void release() {
        localBroadcastManager.unregisterReceiver(this);
        fileTextViewContainer.removeOnAttachStateChangeListener(onAttachStateChangeListener);
    }
}
