package com.lasthopesoftware.bluewater.client.sync.library.items.specs.GivenAnEmptySetOfStoredItems;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.sync.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.sync.library.items.StoredItemsChecker;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.CheckForAnyStoredFiles;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingIfAnyStoredItemsExist {

	private static Boolean isAny;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final IStoredItemAccess storedItemAccess = mock(IStoredItemAccess.class);
		when(storedItemAccess.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.emptyList()));
		final CheckForAnyStoredFiles checkForAnyStoredFiles = mock(CheckForAnyStoredFiles.class);
		when(checkForAnyStoredFiles.promiseIsAnyStoredFiles(any()))
			.thenReturn(new Promise<>(false));
		final StoredItemsChecker storedItemsChecker = new StoredItemsChecker(storedItemAccess, checkForAnyStoredFiles);

		isAny =
			new FuturePromise<>(
				storedItemsChecker.promiseIsAnyStoredItemsOrFiles(new Library()))
			.get(1000, TimeUnit.SECONDS);
	}

	@Test
	public void thenThereAreNotAny() {
		assertThat(isAny).isFalse();
	}
}
