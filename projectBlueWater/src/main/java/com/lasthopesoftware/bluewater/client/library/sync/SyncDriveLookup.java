package com.lasthopesoftware.bluewater.client.library.sync;

import android.content.Context;
import android.os.Environment;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class SyncDriveLookup implements LookupSyncDrive {
	private final Context context;

	public SyncDriveLookup(Context context) {
		this.context = context;
	}

	@Override
	public Promise<File> promiseSyncDrive(Library library) {
		return new Promise<>(
			getExternalFilesDirectoriesStream(library)
				.sortBy(File::getFreeSpace)
				.findLast()
				.orElse(null));
	}

	private Stream<File> getExternalFilesDirectoriesStream(Library library) {
		switch (library.getSyncedFileLocation()) {
			case EXTERNAL:
				return Stream.of(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
			case INTERNAL:
				return Stream.of(context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC))
					.map(f -> new File(f, library.getId() > -1 ? String.valueOf(library.getId()) : ""));
		}

		return Stream.empty();
	}
}
