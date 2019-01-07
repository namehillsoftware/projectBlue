package com.lasthopesoftware.bluewater.client.library.sync;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import io.reactivex.Observable;

public interface CollectServiceFilesForSync {
	Observable<ServiceFile> streamServiceFilesToSync();
}
