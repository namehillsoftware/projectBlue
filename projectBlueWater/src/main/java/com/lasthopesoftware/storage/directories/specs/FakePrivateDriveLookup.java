package com.lasthopesoftware.storage.directories.specs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.storage.directories.GetPrivateDrives;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FakePrivateDriveLookup implements GetPrivateDrives {

	private final List<File> files = new ArrayList<>();

	@Override
	public Promise<Stream<File>> promisePrivateDrives() {
		return new Promise<>(Stream.of(files));
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}

}
