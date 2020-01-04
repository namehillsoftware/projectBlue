package com.lasthopesoftware.bluewater.client.stored.library.specs.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections;

import com.lasthopesoftware.bluewater.client.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingAllLibraries {

	private static final Collection<Library> expectedLibraries = Arrays.asList(
		new Library().setId(5).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
		new Library().setId(4),
		new Library().setId(8),
		new Library().setId(99).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
		new Library().setId(13));
	private static Collection<Library> libraries;

	@BeforeClass
	public static void context() throws ExecutionException, InterruptedException {
		final SyncLibraryProvider syncLibraryProvider = new SyncLibraryProvider(
			new FakeLibraryProvider(Arrays.asList(
				new Library().setId(5).setIsSyncLocalConnectionsOnly(true),
				new Library().setId(4),
				new Library().setId(8),
				new Library().setId(99).setIsSyncLocalConnectionsOnly(true),
				new Library().setId(13))));

		libraries = new FuturePromise<>(syncLibraryProvider.getAllLibraries()).get();
	}

	@Test
	public void thenTheLibraryIsNotLocalOnly() {
		assertThat(libraries).containsExactlyElementsOf(expectedLibraries);
	}
}
