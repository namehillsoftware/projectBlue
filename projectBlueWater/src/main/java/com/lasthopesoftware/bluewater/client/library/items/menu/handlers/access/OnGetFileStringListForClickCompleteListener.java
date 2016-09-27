package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickCompleteListener implements TwoParameterRunnable<IFluentTask<String,Void,String>, String> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public void run(IFluentTask<String,Void,String> owner, String result) {
        PlaybackService.launchMusicService(mContext, result);
    }
}
