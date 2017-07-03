package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promise.Promise;

/**
 * Created by david on 11/2/16.
 */

public interface INowPlayingFileProvider {
	Promise<ServiceFile> getNowPlayingFile();
}
