package com.lasthopesoftware.bluewater.client.library.sync;

import android.os.StatFs;

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
		return getExternalFilesDirectoriesStream(library)
			.then(files ->
				files.sortBy(f -> {
					final StatFs statFs = new StatFs(f.getPath());
					return statFs.getFreeBytes();
				})
				.findLast()
				.orElse(null));
	}

	private Promise<Stream<File>> getExternalFilesDirectoriesStream(Library library) {
		switch (library.getSyncedFileLocation()) {
			case EXTERNAL:
				return publicDrives.promisePublicDrives();
			case INTERNAL:
				final String libraryId = library.getId() > -1 ? String.valueOf(library.getId()) : "";
				return privateDrives.promisePrivateDrives()
					.then(files -> files.map(f -> new File(f, libraryId)));
		}

		return new Promise<>(Stream.empty());
	}
}
