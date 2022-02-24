package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems.AndCollectionIsCancelledAfterStoredItemsAreReturned;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeDeferredStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WhenCollectingTheAssociatedServiceFiles {

	private static final List<ServiceFile> firstItemExpectedFiles = givenARandomCollectionOfFiles();
	private static final List<ServiceFile> secondItemExpectedFiles = givenARandomCollectionOfFiles();
	private static final List<ServiceFile> thirdItemExpectedFiles = givenARandomCollectionOfFiles();
	private static Throwable exception;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException {

		final FakeDeferredStoredItemAccess storedItemAccess = new FakeDeferredStoredItemAccess() {
			@Override
			protected Collection<StoredItem> getStoredItems() {
				return Arrays.asList(
					new StoredItem(1, 1, StoredItem.ItemType.ITEM),
					new StoredItem(1, 2, StoredItem.ItemType.ITEM),
					new StoredItem(1, 3, StoredItem.ItemType.ITEM));
			}
		};

		final FileListParameters fileListParameters = FileListParameters.INSTANCE;

		final ProvideLibraryFiles fileProvider = mock(ProvideLibraryFiles.class);
		when(fileProvider.promiseFiles(new LibraryId(2), FileListParameters.Options.None, fileListParameters.getFileListParameters(new ItemId(1))))
			.thenReturn(new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(new LibraryId(2), FileListParameters.Options.None, fileListParameters.getFileListParameters(new ItemId(2))))
			.thenReturn(new Promise<>(secondItemExpectedFiles));
		when(fileProvider.promiseFiles(new LibraryId(2), FileListParameters.Options.None, fileListParameters.getFileListParameters(new ItemId(3))))
			.thenReturn(new Promise<>(thirdItemExpectedFiles));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			fileListParameters);

		final Promise<Collection<ServiceFile>> serviceFilesPromise = serviceFileCollector.promiseServiceFilesToSync(new LibraryId(2));

		serviceFilesPromise.cancel();

		storedItemAccess.resolveStoredItems();

		try {
			new FuturePromise<>(serviceFilesPromise).get(1, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			exception = e.getCause();
		}
	}

	@Test
	public void thenACancellationExceptionIsThrown() {
		assertThat(exception).isInstanceOf(CancellationException.class);
	}

	private static List<ServiceFile> givenARandomCollectionOfFiles() {
		final Random random = new Random();
		final int floor = random.nextInt(10000);
		final int ceiling = random.nextInt(10000 - floor) + floor;
		return Stream.range(floor, ceiling).map(ServiceFile::new).toList();
	}

}
