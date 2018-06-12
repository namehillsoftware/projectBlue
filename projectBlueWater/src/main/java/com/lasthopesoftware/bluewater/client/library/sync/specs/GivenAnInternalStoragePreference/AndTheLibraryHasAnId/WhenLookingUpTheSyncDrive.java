package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenAnInternalStoragePreference.AndTheLibraryHasAnId;

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
		final FakePrivateDriveLookup fakePrivateDriveLookup = new FakePrivateDriveLookup();
		fakePrivateDriveLookup.addDirectory("", 1);
		fakePrivateDriveLookup.addDirectory("", 2);
		fakePrivateDriveLookup.addDirectory("", 3);
		fakePrivateDriveLookup.addDirectory("/storage/0/my-private-sd-card", 10);

		final FakePublicDriveLookup publicDrives = new FakePublicDriveLookup();
		publicDrives.addDirectory("fake-private-path", 12);
		publicDrives.addDirectory("/fake-private-path", 5);

		final SyncDriveLookup syncDriveLookup = new SyncDriveLookup(
			publicDrives,
			fakePrivateDriveLookup);

		file = new FuturePromise<>(
			syncDriveLookup.promiseSyncDrive(new Library()
				.setId(14)
				.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-private-sd-card/14");
	}
}
