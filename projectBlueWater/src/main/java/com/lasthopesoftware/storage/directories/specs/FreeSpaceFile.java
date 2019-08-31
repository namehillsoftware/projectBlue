package com.lasthopesoftware.storage.directories.specs;

import androidx.annotation.NonNull;

import java.io.File;

class FreeSpaceFile extends File {

	private final long freeSpace;

	FreeSpaceFile(@NonNull String pathname, long freeSpace) {
		super(pathname);
		this.freeSpace = freeSpace;
	}

	@Override
	public long getFreeSpace() {
		return freeSpace;
	}
}
