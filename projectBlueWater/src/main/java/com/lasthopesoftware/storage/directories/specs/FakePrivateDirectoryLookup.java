package com.lasthopesoftware.storage.directories.specs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.storage.GetFreeSpace;
import com.lasthopesoftware.storage.directories.GetPrivateDirectories;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakePrivateDirectoryLookup implements GetPrivateDirectories, GetFreeSpace {

	private final List<File> files = new ArrayList<>();

	@NotNull
	@Override
	public Promise<Collection<File>> promisePrivateDrives() {
		return new Promise<>(files);
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}

	@Override
	public long getFreeSpace(@NotNull File file) {
		return Stream.of(files).filter(f -> f.getPath().startsWith(file.getPath())).findFirst().mapToLong(File::getFreeSpace).orElse(0);
	}
}
