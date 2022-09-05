package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats;

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IPlaystatsUpdate {
	Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile);
}
