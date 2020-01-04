package com.lasthopesoftware.bluewater.client.stored.library.specs.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections;

import com.lasthopesoftware.bluewater.client.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingALibraryThatSyncsOnLocalConnection {

	private static Library library;

	@BeforeClass
	public static void context() throws ExecutionException, InterruptedException {
		final SyncLibraryProvider syncLibraryProvider = new SyncLibraryProvider(
			new FakeLibraryProvider(Arrays.asList(
				new Library().setId(3),
				new Library().setId(4),
				new Library().setId(8).setIsSyncLocalConnectionsOnly(true),
				new Library().setId(1)
			)));

		library = new FuturePromise<>(syncLibraryProvider.getLibrary(new LibraryId(8))).get();
	}

	@Test
	public void thenTheLibraryIsLocalOnly() {
		assertThat(library.isLocalOnly()).isTrue();
	}
}
