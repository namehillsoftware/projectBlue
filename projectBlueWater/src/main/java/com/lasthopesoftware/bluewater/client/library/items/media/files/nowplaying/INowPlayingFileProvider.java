package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/2/16.
 */

public interface INowPlayingFileProvider {
	IPromise<IFile> getNowPlayingFile();
}
