package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.threading.ISimpleTask;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickCompleteListener implements ISimpleTask.OnCompleteListener<String, Void, String> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
        PlaybackService.launchMusicService(mContext, result);
    }
}
