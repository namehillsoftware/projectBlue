package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickCompleteListener implements OneParameterRunnable<String> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public void run(String result) {
        PlaybackService.launchMusicService(mContext, result);
    }
}
