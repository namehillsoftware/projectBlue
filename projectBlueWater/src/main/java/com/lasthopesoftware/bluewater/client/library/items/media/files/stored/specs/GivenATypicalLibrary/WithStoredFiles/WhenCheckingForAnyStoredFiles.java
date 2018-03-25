package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.specs.GivenATypicalLibrary.WithStoredFiles;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFilesChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCheckingForAnyStoredFiles {

	private static Boolean isAny;

	@BeforeClass
	public static void before() {
		final GetAllStoredFilesInLibrary getAllStoredFilesInLibrary = mock(GetAllStoredFilesInLibrary.class);
		when(getAllStoredFilesInLibrary.promiseAllStoredFiles(any()))
			.thenReturn(new Promise<>(Collections.singleton(new StoredFile())));

		final StoredFilesChecker storedFilesChecker = new StoredFilesChecker(getAllStoredFilesInLibrary);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		storedFilesChecker.promiseIsAnyStoredFiles(new Library())
			.then(any -> {
				isAny = any;
				countDownLatch.countDown();
				return null;
			});
	}

	@Test
	public void thenATrueResultIsGiven() {
		assertThat(isAny).isTrue();
	}
}
