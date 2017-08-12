package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats;

import com.lasthopesoftware.messenger.promises.Promise;

public interface IPlaystatsUpdate {
	Promise<Boolean> promisePlaystatsUpdate(int fileKey);
}
