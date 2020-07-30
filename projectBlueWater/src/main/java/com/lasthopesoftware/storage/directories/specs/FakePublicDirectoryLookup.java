package com.lasthopesoftware.storage.directories.specs;

import com.lasthopesoftware.storage.directories.GetPublicDirectories;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakePublicDirectoryLookup implements GetPublicDirectories {

	private final List<File> files = new ArrayList<>();

	@NotNull
	@Override
	public Promise<Collection<File>> promisePublicDrives() {
		return new Promise<>(files);
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}
}
