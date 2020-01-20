package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.FileListParameters;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public interface ProvideFiles {
	Promise<List<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params);
}
