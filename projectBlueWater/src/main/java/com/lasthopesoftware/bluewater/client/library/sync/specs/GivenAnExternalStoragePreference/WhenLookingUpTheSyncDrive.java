package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenAnExternalStoragePreference;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.sync.SyncDriveLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.storage.directories.specs.FakePrivateDriveLookup;
import com.lasthopesoftware.storage.directories.specs.FakePublicDriveLookup;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePublicDriveLookup publicDrives = new FakePublicDriveLookup();
		publicDrives.addDirectory("", 1);
		publicDrives.addDirectory("", 2);
		publicDrives.addDirectory("", 3);
		publicDrives.addDirectory("/storage/0/my-big-sd-card", 4);

		final FakePrivateDriveLookup fakePrivateDriveLookup = new FakePrivateDriveLookup();
		fakePrivateDriveLookup.addDirectory("fake-private-path", 3);
		fakePrivateDriveLookup.addDirectory("/fake-private-path", 5);

		final SyncDriveLookup syncDriveLookup = new SyncDriveLookup(
			publicDrives,
			fakePrivateDriveLookup);

		file = new FuturePromise<>(
			syncDriveLookup.promiseSyncDrive(new Library()
				.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-big-sd-card");
	}
}
