package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenASetOfStoredItems.AndCollectionIsCancelledAfterStoredItemsAreReturned;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static List<ServiceFile> firstItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> secondItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> thirdItemExpectedFiles = givenARandomCollectionOfFiles();
	private static Throwable exception;

	@BeforeClass
	public static void before() throws InterruptedException {

		final DeferredStoredItemAccess storedItemAccess = new DeferredStoredItemAccess();

		final IFileProvider fileProvider = mock(IFileProvider.class);
		when(fileProvider.promiseFiles(FileListParameters.Options.None, "Browse/Files", "ID=1"))
			.thenAnswer(e -> new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, "Browse/Files", "ID=2"))
			.thenAnswer(e -> new Promise<>(secondItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, "Browse/Files", "ID=3"))
			.thenAnswer(e -> new Promise<>(thirdItemExpectedFiles));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final Promise<Collection<ServiceFile>> serviceFilesPromise = serviceFileCollector.promiseServiceFilesToSync();
		serviceFilesPromise
			.then(files -> {
				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				exception = e;
				countDownLatch.countDown();
				return null;
			});

		serviceFilesPromise.cancel();

		storedItemAccess.resolveStoredItems();

		countDownLatch.await();
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

	private static class DeferredStoredItemAccess implements IStoredItemAccess {

		private Messenger<Collection<StoredItem>> messenger;

		void resolveStoredItems() {
			if (messenger != null) {
				messenger.sendResolution(Arrays.asList(
					new StoredItem(1, 1, StoredItem.ItemType.ITEM),
					new StoredItem(1, 2, StoredItem.ItemType.ITEM),
					new StoredItem(1, 3, StoredItem.ItemType.ITEM)));
			}
		}

		@Override
		public void toggleSync(IItem item, boolean enable) {

		}

		@Override
		public Promise<Boolean> isItemMarkedForSync(IItem item) {
			return new Promise<>(false);
		}

		@Override
		public Promise<Collection<StoredItem>> promiseStoredItems() {
			return new Promise<>((m -> messenger = m));
		}
	}
}
