package com.lasthopesoftware.bluewater.client.library.sync;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.directories.GetPrivateDrives;
import com.lasthopesoftware.storage.directories.GetPublicDrives;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class SyncDriveLookup implements LookupSyncDrive {
	private final GetPublicDrives publicDrives;
	private final GetPrivateDrives privateDrives;

	public SyncDriveLookup(GetPublicDrives publicDrives, GetPrivateDrives privateDrives) {

		this.publicDrives = publicDrives;
		this.privateDrives = privateDrives;
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
//		switch (library.getSyncedFileLocation()) {
//			case EXTERNAL:
//				return Stream.of(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
//			case INTERNAL:
//				final String libraryId = library.getId() > -1 ? String.valueOf(library.getId()) : "";
//				return Stream.of(context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC))
//					.map(f -> new File(f, libraryId));
//		}

		return Stream.empty();
	}
}
