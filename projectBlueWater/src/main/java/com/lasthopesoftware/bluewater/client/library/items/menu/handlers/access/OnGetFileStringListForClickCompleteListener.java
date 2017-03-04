package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickCompleteListener implements OneParameterAction<String> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public void runWith(String result) {
        PlaybackService.launchMusicService(mContext, result);
    }
}
