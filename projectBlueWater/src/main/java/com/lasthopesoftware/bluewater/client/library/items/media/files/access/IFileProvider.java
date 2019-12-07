package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface IFileProvider {
	Promise<Collection<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params);
}
