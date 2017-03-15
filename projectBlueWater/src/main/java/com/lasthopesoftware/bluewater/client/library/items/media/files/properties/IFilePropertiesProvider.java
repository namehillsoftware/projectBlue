package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.promises.IPromise;

import java.util.Map;

/**
 * Created by david on 3/14/17.
 */

public interface IFilePropertiesProvider {
	IPromise<Map<String, String>> promiseFileProperties(int fileKey);
}
