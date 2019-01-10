package com.lasthopesoftware.bluewater.client.sync.library;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.directories.GetPrivateDirectories;
import com.lasthopesoftware.storage.directories.GetPublicDirectories;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class SyncDirectoryLookup implements LookupSyncDirectory {
	private final GetPublicDirectories publicDrives;
	private final GetPrivateDirectories privateDrives;

	public SyncDirectoryLookup(GetPublicDirectories publicDrives, GetPrivateDirectories privateDrives) {
		this.publicDrives = publicDrives;
		this.privateDrives = privateDrives;
	}

	@Override
	public Promise<File> promiseSyncDirectory(Library library) {
		return getExternalFilesDirectoriesStream(library)
			.then(files ->
				files.sortBy(File::getFreeSpace)
				.findLast()
				.orElse(null));
	}

	private Promise<Stream<File>> getExternalFilesDirectoriesStream(Library library) {
		switch (library.getSyncedFileLocation()) {
			case EXTERNAL:
				return promiseDirectoriesWithLibrary(library, publicDrives.promisePublicDrives());
			case INTERNAL:
				return promiseDirectoriesWithLibrary(library, privateDrives.promisePrivateDrives());
			case CUSTOM:
				return new Promise<>(Stream.of(new File(library.getCustomSyncedFilesPath())));
		}

		return new Promise<>(Stream.empty());
	}

	private static Promise<Stream<File>> promiseDirectoriesWithLibrary(Library library, Promise<Stream<File>> promisedDirectories) {
		if (library.getId() < 0) return promisedDirectories;

		final String libraryId = String.valueOf(library.getId());
		return promisedDirectories
			.then(files -> files.map(f -> new File(f, libraryId)));
	}
}
