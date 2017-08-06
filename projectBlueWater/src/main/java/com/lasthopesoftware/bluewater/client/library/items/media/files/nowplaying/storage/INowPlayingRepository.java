package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.messenger.promises.Promise;

public interface INowPlayingRepository {
	Promise<NowPlaying> getNowPlaying();

	Promise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying);
}
