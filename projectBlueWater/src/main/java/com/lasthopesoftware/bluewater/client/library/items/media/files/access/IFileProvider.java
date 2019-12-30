package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public interface IFileProvider {
	Promise<List<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params);
	Promise<List<ServiceFile>> promiseFiles(LibraryId libraryId, FileListParameters.Options option, String... params);
}
