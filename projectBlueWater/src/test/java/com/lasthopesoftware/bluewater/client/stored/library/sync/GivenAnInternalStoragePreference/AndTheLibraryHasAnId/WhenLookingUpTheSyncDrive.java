package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnInternalStoragePreference.AndTheLibraryHasAnId;

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.FakePublicDirectoryLookup;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePrivateDirectoryLookup fakePrivateDirectoryLookup = new FakePrivateDirectoryLookup();
		fakePrivateDirectoryLookup.addDirectory("", 1);
		fakePrivateDirectoryLookup.addDirectory("", 2);
		fakePrivateDirectoryLookup.addDirectory("", 3);
		fakePrivateDirectoryLookup.addDirectory("/storage/0/my-private-sd-card", 10);

		final FakePublicDirectoryLookup publicDrives = new FakePublicDirectoryLookup();
		publicDrives.addDirectory("fake-private-path", 12);
		publicDrives.addDirectory("/fake-private-path", 5);

		final FakeLibraryProvider fakeLibraryProvider = new FakeLibraryProvider(new Library()
			.setId(14)
			.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL));

		final SyncDirectoryLookup syncDirectoryLookup = new SyncDirectoryLookup(
			fakeLibraryProvider,
			publicDrives,
			fakePrivateDirectoryLookup,
			fakePrivateDirectoryLookup);

		file = new FuturePromise<>(syncDirectoryLookup.promiseSyncDirectory(new LibraryId(14))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-private-sd-card/14");
	}
}
