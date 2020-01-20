package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.storage;

import com.namehillsoftware.handoff.promises.Promise;

public interface INowPlayingRepository {
	Promise<NowPlaying> getNowPlaying();

	Promise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying);
}
