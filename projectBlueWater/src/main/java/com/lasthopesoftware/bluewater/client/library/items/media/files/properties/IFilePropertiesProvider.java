package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Map;

/**
 * Created by david on 3/14/17.
 */

public interface IFilePropertiesProvider {
	Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile);
}
