package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters.IPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.callables.VoidFunc;

/**
 * Created by david on 10/11/15.
 */
public class NowPlayingFloatingActionButton extends FloatingActionButton {
    public static NowPlayingFloatingActionButton addNowPlayingFloatingActionButton(RelativeLayout container) {
        final NowPlayingFloatingActionButton nowPlayingFloatingActionButton = new NowPlayingFloatingActionButton(container.getContext());

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? RelativeLayout.ALIGN_PARENT_END : RelativeLayout.ALIGN_PARENT_RIGHT);
        final int margin = ViewUtils.dpToPx(container.getContext(), 16);
        layoutParams.setMargins(margin, margin, margin, margin);

        nowPlayingFloatingActionButton.setLayoutParams(layoutParams);
        container.addView(nowPlayingFloatingActionButton);

        return nowPlayingFloatingActionButton;
    }

    private boolean isNowPlayingFileSet;

    private NowPlayingFloatingActionButton(Context context) {
        super(context);

        setImageDrawable(ViewUtils.getDrawable(context, R.drawable.av_play_dark));

        initializeNowPlayingFloatingActionButton();
    }


    @SuppressWarnings("ResourceType")
    private void initializeNowPlayingFloatingActionButton() {
        setOnClickListener(v -> NowPlayingActivity.startNowPlayingActivity(v.getContext()));

        setVisibility(ViewUtils.getVisibility(false));
        // The user can change the library, so let's check if the state of visibility on the
        // now playing menu item should change
        LibrarySession.getActiveLibrary(getContext()).then(VoidFunc.runningCarelessly(result -> {
            isNowPlayingFileSet = result != null && result.getNowPlayingId() >= 0;
            setVisibility(ViewUtils.getVisibility(isNowPlayingFileSet));

            if (isNowPlayingFileSet) return;

            final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());

            localBroadcastManager.registerReceiver(new BroadcastReceiver() {
                @Override
                public synchronized void onReceive(Context context, Intent intent) {
                    isNowPlayingFileSet = true;
                    setVisibility(ViewUtils.getVisibility(true));
                    localBroadcastManager.unregisterReceiver(this);
                }
            }, new IntentFilter(IPlaybackBroadcaster.PlaylistEvents.onPlaylistStart));
        }));
    }

    @Override
    public void show() {
        if (isNowPlayingFileSet) super.show();
    }

    @Override
    public void show(OnVisibilityChangedListener listener) {
        if (isNowPlayingFileSet) super.show(listener);
    }
}
