package com.lasthopesoftware.bluewater.client.connection.builder.live;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface ProvideLiveUrl {
	Promise<IUrlProvider> promiseLiveUrl(Library library);
}
