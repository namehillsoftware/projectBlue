package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenAnEmptySetOfServiceFiles;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.annimon.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;
	private static List<ServiceFile> firstItemExpectedFiles = Collections.emptyList();
	private static List<ServiceFile> secondItemExpectedFiles = Collections.emptyList();
	private static List<ServiceFile> thirdItemExpectedFiles = Collections.emptyList();

	@BeforeClass
	public static void before() throws InterruptedException {

		final IStoredItemAccess storedItemAccess =
			new IStoredItemAccess() {
				@Override
				public void toggleSync(IItem item, boolean enable) {
				}

				@Override
				public Promise<Boolean> isItemMarkedForSync(IItem item) {
					return null;
				}

				@Override
				public Promise<Collection<StoredItem>> promiseStoredItems() {
					return new Promise<>(Arrays.asList(
						new StoredItem(1, 1, StoredItem.ItemType.ITEM),
						new StoredItem(1, 2, StoredItem.ItemType.ITEM),
						new StoredItem(1, 3, StoredItem.ItemType.ITEM)));
				}
			};

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
		serviceFileCollector
			.promiseServiceFilesToSync()
			.then(files -> {
				collectedFiles = files;
				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenOnlyTheFoundServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(new HashSet<>(concat(Stream.of(firstItemExpectedFiles), Stream.of(thirdItemExpectedFiles)).toList()));
	}
}
