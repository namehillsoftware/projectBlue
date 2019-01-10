package com.lasthopesoftware.bluewater.client.sync.library.specs.GivenACustomStoragePreference;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.sync.library.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.storage.directories.specs.FakePrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.specs.FakePublicDirectoryLookup;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePublicDirectoryLookup publicDrives = new FakePublicDirectoryLookup();
		publicDrives.addDirectory("", 1);
		publicDrives.addDirectory("", 2);
		publicDrives.addDirectory("", 3);
		publicDrives.addDirectory("/storage/0/my-big-sd-card", 4);

		final FakePrivateDirectoryLookup fakePrivateDirectoryLookup = new FakePrivateDirectoryLookup();
		fakePrivateDirectoryLookup.addDirectory("fake-private-path", 3);
		fakePrivateDirectoryLookup.addDirectory("/fake-private-path", 5);

		final SyncDirectoryLookup syncDirectoryLookup = new SyncDirectoryLookup(
			publicDrives,
			fakePrivateDirectoryLookup);

		file = new FuturePromise<>(
			syncDirectoryLookup.promiseSyncDirectory(new Library()
				.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM)
				.setCustomSyncedFilesPath("/stoarage2/my-second-card"))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/stoarage2/my-second-card");
	}
}
