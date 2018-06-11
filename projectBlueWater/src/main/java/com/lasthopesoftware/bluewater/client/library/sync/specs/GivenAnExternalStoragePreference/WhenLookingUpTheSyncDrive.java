package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenAnExternalStoragePreference;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.sync.SyncDriveLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.storage.directories.GetPrivateDrives;
import com.lasthopesoftware.storage.directories.GetPublicDrives;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SyncDriveLookup syncDriveLookup = new SyncDriveLookup(
			mock(GetPublicDrives.class),
			mock(GetPrivateDrives.class));

		file = new FuturePromise<>(syncDriveLookup.promiseSyncDrive(new Library())).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-big-sd-card");
	}
}
