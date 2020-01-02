package com.lasthopesoftware.storage.directories.specs;

import com.lasthopesoftware.storage.directories.GetPublicDirectories;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FakePublicDirectoryLookup implements GetPublicDirectories {

	private final List<File> files = new ArrayList<>();

	@Override
	public Promise<List<File>> promisePublicDrives() {
		return new Promise<>(files);
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}
}
