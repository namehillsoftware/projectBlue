package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenAnEmptySetOfStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;

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
					return new Promise<>(Collections.emptyList());
				}
			};

		final IFileProvider fileProvider = mock(IFileProvider.class);

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
	public void thenNoServiceFilesAreReturned() {
		assertThat(collectedFiles).isEmpty();
	}
}
