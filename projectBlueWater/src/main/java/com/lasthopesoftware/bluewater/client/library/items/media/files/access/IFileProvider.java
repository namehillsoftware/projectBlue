package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.List;

public interface IFileProvider {
	Promise<List<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params);
}
