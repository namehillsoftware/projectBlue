package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenASetOfStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.CheckForAnyStoredFiles;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingIfAnyStoredItemsExist {

	private static Boolean isAny;

	@BeforeClass
	public static void before() throws InterruptedException {
		final IStoredItemAccess storedItemAccess = mock(IStoredItemAccess.class);
		when(storedItemAccess.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.singleton(new StoredItem())));
		final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, mock(CheckForAnyStoredFiles.class));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		storedItemsChecker.promiseIsAnyStoredItemsOrFiles(new Library())
			.then(r -> {
				isAny = r;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThereAreSome() {
		assertThat(isAny).isTrue();
	}
}
