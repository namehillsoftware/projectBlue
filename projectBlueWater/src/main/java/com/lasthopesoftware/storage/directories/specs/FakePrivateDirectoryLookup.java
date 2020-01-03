package com.lasthopesoftware.storage.directories.specs;

import com.lasthopesoftware.storage.directories.GetPrivateDirectories;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakePrivateDirectoryLookup implements GetPrivateDirectories {

	private final List<File> files = new ArrayList<>();

	@Override
	public Promise<Collection<File>> promisePrivateDrives() {
		return new Promise<>(files);
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}

}
