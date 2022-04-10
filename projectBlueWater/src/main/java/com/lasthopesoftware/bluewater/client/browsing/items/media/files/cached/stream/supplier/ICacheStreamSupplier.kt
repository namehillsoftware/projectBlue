package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream;
import com.namehillsoftware.handoff.promises.Promise;

public interface ICacheStreamSupplier {
	Promise<CacheOutputStream> promiseCachedFileOutputStream(final String uniqueKey);
}
