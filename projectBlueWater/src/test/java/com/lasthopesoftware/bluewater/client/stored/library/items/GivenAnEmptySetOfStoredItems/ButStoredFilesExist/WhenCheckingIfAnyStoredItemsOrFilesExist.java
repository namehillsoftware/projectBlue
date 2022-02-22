package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfStoredItems.ButStoredFilesExist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems;
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

public class WhenCheckingIfAnyStoredItemsOrFilesExist {

	private static Boolean isAny;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final AccessStoredItems storedItemAccess = mock(AccessStoredItems.class);
		when(storedItemAccess.promiseStoredItems(new LibraryId(10)))
			.thenReturn(new Promise<>(Collections.emptyList()));
		final CheckForAnyStoredFiles checkForAnyStoredFiles = mock(CheckForAnyStoredFiles.class);
		when(checkForAnyStoredFiles.promiseIsAnyStoredFiles(any()))
			.thenReturn(new Promise<>(true));

		final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, checkForAnyStoredFiles);

		isAny =
			new FuturePromise<>(storedItemsChecker.promiseIsAnyStoredItemsOrFiles(new LibraryId(10)))
				.get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenATrueResultIsReturned() {
		assertThat(isAny).isTrue();
	}
}
