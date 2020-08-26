package com.lasthopesoftware.storage.directories.specs;

import androidx.annotation.NonNull;

import java.io.File;

final class FreeSpaceFile extends File {

	private String pathname;
	private final long freeSpace;

	FreeSpaceFile(@NonNull String pathname, long freeSpace) {
		super(pathname);
		this.pathname = pathname;
		this.freeSpace = freeSpace;
	}

	@NonNull
	@Override
	public String getPath() {
		return pathname;
	}

	@Override
	public long getFreeSpace() {
		return freeSpace;
	}
}
