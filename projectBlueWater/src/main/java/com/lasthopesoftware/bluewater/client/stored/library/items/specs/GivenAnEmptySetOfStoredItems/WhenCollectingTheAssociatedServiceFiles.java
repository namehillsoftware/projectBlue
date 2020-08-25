package com.lasthopesoftware.bluewater.client.stored.library.items.specs.GivenAnEmptySetOfStoredItems;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {

		final IStoredItemAccess storedItemAccess =
			new IStoredItemAccess() {
				@NotNull
				@Override
				public Promise<?> disableAllLibraryItems(@NotNull LibraryId libraryId) {
					return null;
				}

				@Override
				public void toggleSync(LibraryId libraryId, IItem item, boolean enable) {
				}

				@Override
				public Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, IItem item) {
					return null;
				}

				@Override
				public Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId) {
					return new Promise<>(Collections.emptyList());
				}
			};

		final ProvideLibraryFiles fileProvider = mock(ProvideLibraryFiles.class);

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			FileListParameters.getInstance());

		collectedFiles =
			new FuturePromise<>(serviceFileCollector.promiseServiceFilesToSync(new LibraryId(14)))
				.get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenNoServiceFilesAreReturned() {
		assertThat(collectedFiles).isEmpty();
	}
}
