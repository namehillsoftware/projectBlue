package com.lasthopesoftware.bluewater.client.stored.library.items.files.specs.GivenATypicalLibrary.WithNoStoredFiles;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CountStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesChecker;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingForAnyStoredFiles {

	private static Boolean isAny;

	@BeforeClass
	public static void before() {
		final CountStoredFiles countStoredFilesInLibrary = mock(CountStoredFiles.class);
		when(countStoredFilesInLibrary.promiseStoredFilesCount(any()))
			.thenReturn(new Promise<>(0L));

		final StoredFilesChecker storedFilesChecker = new StoredFilesChecker(countStoredFilesInLibrary);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		storedFilesChecker.promiseIsAnyStoredFiles(new LibraryId(3))
			.then(any -> {
				isAny = any;
				countDownLatch.countDown();
				return null;
			});
	}

	@Test
	public void thenAFalseResultIsGiven() {
		assertThat(isAny).isFalse();
	}
}
