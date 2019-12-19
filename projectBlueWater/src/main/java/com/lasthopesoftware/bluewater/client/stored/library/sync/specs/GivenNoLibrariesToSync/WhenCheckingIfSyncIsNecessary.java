package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenNoLibrariesToSync;

import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCheckingIfSyncIsNecessary {

	private static boolean isSyncNeeded;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SyncChecker syncChecker = new SyncChecker();
		isSyncNeeded = new FuturePromise<>(syncChecker.promiseIsSyncNeeded()).get();
	}

	@Test
	public void thenSyncIsNotNeeded() {
		assertThat(isSyncNeeded).isFalse();
	}
}
