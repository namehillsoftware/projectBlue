package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.messenger.promise.Promise;

/**
 * Created by david on 1/29/17.
 */

public interface INowPlayingRepository {
	Promise<NowPlaying> getNowPlaying();

	Promise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying);
}
