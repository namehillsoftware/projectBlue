package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public final class OnGetFileStringListForClickCompleteListener implements CarelessOneParameterFunction<String, Void> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public Void resultFrom(String result) throws Throwable {
        PlaybackService.launchMusicService(mContext, result);
        return null;
    }
}
