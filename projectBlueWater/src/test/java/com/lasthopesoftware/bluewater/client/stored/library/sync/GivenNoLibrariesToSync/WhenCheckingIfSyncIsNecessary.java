package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenNoLibrariesToSync;

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCheckingIfSyncIsNecessary {

	private static boolean isSyncNeeded;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SyncChecker syncChecker = new SyncChecker(
			new FakeLibraryProvider(
				new Library().setId(3),
				new Library().setId(11),
				new Library().setId(10),
				new Library().setId(14)),
			(l) -> new Promise<>(Collections.emptySet()));
		isSyncNeeded = new FuturePromise<>(syncChecker.promiseIsSyncNeeded()).get();
	}

	@Test
	public void thenSyncIsNotNeeded() {
		assertThat(isSyncNeeded).isFalse();
	}
}
