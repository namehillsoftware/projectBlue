package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

/**
 * Created by david on 4/14/15.
 */
public abstract class AbstractFileListItemNowPlayingHandler extends BroadcastReceiver {

    private final RelativeLayout fileTextViewContainer;

    private final View.OnAttachStateChangeListener onAttachStateChangeListener;

    private final LocalBroadcastManager localBroadcastManager;

    public AbstractFileListItemNowPlayingHandler(FileListItemContainer fileListItem) {
        fileTextViewContainer = fileListItem.getTextViewContainer();

        localBroadcastManager = LocalBroadcastManager.getInstance(fileTextViewContainer.getContext());

        localBroadcastManager.registerReceiver(this, new IntentFilter(PlaylistEvents.onPlaylistChange));

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
