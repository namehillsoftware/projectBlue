package com.lasthopesoftware.storage.directories;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.storage.GetFreeSpace;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakePublicDirectoryLookup implements GetPublicDirectories, GetFreeSpace {

	private final List<FreeSpaceFile> files = new ArrayList<>();

	@NotNull
	@Override
	public Promise<Collection<File>> promisePublicDrives() {
		return new Promise<>(Stream.of(files).map(f -> (File)f).toList());
	}

	public void addDirectory(String filePath, long freeSpace) {
		files.add(new FreeSpaceFile(filePath, freeSpace));
	}

	@Override
	public long getFreeSpace(@NotNull File file) {
		String path = file.getPath();

		List<String> filePaths = Stream.of(files).map(FreeSpaceFile::getPath).toList();
		while(!filePaths.contains(path)) {
			int pathSeparatorIndex = path.lastIndexOf('/');
			if (pathSeparatorIndex < 0) return 0;
			path = path.substring(0, pathSeparatorIndex);
		}

		final String matchingPath = path;
		final Optional<FreeSpaceFile> freeSpaceFile = Stream.of(files).filter(f -> f.getPath().equals(matchingPath)).findFirst();
		return freeSpaceFile.mapToLong(FreeSpaceFile::getFreeSpace).orElse(0);
	}
}
