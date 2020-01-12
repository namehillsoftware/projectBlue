package com.lasthopesoftware.bluewater.client.stored.library.specs.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections;

import com.lasthopesoftware.bluewater.client.browsing.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingALibraryThatDoesSyncsOnAnyConnection {

	private static Library library;

	@BeforeClass
	public static void context() throws ExecutionException, InterruptedException {
		final SyncLibraryProvider syncLibraryProvider = new SyncLibraryProvider(
			new FakeLibraryProvider(Arrays.asList(
				new Library().setId(3),
				new Library().setId(4),
				new Library().setId(8).setIsSyncLocalConnectionsOnly(true),
				new Library().setId(1),
				new Library().setId(13).setIsSyncLocalConnectionsOnly(true)
			)));

		library = new FuturePromise<>(syncLibraryProvider.getLibrary(new LibraryId(4))).get();
	}

	@Test
	public void thenTheLibraryIsNotLocalOnly() {
		assertThat(library.isLocalOnly()).isFalse();
	}
}
