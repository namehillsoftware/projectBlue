package com.lasthopesoftware.bluewater.client.connection.builder.live;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;

public interface ProvideLiveUrl {
	Promise<IUrlProvider> promiseLiveUrl(Library library);
}
