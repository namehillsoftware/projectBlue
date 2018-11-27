package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenAnEmptySetOfStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.CheckForAnyStoredFiles;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingIfAnyStoredItemsExist {

	private static Boolean isAny;

	@BeforeClass
	public static void before() throws InterruptedException {
		final IStoredItemAccess storedItemAccess = mock(IStoredItemAccess.class);
		when(storedItemAccess.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.emptyList()));
		final CheckForAnyStoredFiles checkForAnyStoredFiles = mock(CheckForAnyStoredFiles.class);
		when(checkForAnyStoredFiles.promiseIsAnyStoredFiles(any()))
			.thenReturn(new Promise<>(false));
		final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, checkForAnyStoredFiles);

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
	public void thenThereAreNotAny() {
		assertThat(isAny).isFalse();
	}
}
