package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 1/29/17.
 */

public interface INowPlayingRepository {
	IPromise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying);
	IPromise<NowPlaying> getNowPlaying();
}
