package com.lasthopesoftware.bluewater.client.stored.service.receivers.specs.GivenAFileReadPermissionsError.AndReadPermissionsAreNotGranted;

import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.StoredFileReadPermissionsReceiver;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingTheNotification {

	private static final List<Integer> requestedReadPermissionLibraries = new LinkedList<>();

	@BeforeClass
	public static void before() throws Exception {
		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.getStoredFile(14))
			.thenReturn(new Promise<>(new StoredFile().setId(14).setLibraryId(22)));

		final StoredFileReadPermissionsReceiver storageReadPermissionsRequestedBroadcaster =
			new StoredFileReadPermissionsReceiver(
				() -> false,
				requestedReadPermissionLibraries::add,
				storedFileAccess);

		new FuturePromise<>(storageReadPermissionsRequestedBroadcaster.receive(14)).get();
	}

	@Test
	public void thenAReadPermissionsRequestIsSent() {
		assertThat(requestedReadPermissionLibraries).containsOnly(22);
	}
}
