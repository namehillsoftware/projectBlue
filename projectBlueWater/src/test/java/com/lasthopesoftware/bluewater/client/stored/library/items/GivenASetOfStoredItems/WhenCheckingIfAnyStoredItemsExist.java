package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingIfAnyStoredItemsExist {

	private static Boolean isAny;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {
		final IStoredItemAccess storedItemAccess = mock(IStoredItemAccess.class);
		when(storedItemAccess.promiseStoredItems(new LibraryId(10)))
			.thenReturn(new Promise<>(Collections.singleton(new StoredItem())));
		final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, mock(CheckForAnyStoredFiles.class));

		isAny = new FuturePromise<>(
			storedItemsChecker
				.promiseIsAnyStoredItemsOrFiles(new LibraryId(10)))
			.get(1000, TimeUnit.SECONDS);
	}

	@Test
	public void thenThereAreSome() {
		assertThat(isAny).isTrue();
	}
}
