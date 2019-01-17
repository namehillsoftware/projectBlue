package com.lasthopesoftware.bluewater.client.stored.service.receivers.specs.GivenADownloadedFile;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.StoredFileMediaScannerNotifier;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenNotifyingTheMediaScanner {

	private static final List<File> collectedFiles = new ArrayList<>();

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.getStoredFile(14))
			.thenReturn(new Promise<>(new StoredFile().setId(14).setLibraryId(22).setPath("test")));

		final StoredFileMediaScannerNotifier storedFileMediaScannerNotifier = new StoredFileMediaScannerNotifier(
			storedFileAccess,
			collectedFiles::add);

		new FuturePromise<>(storedFileMediaScannerNotifier.receive(14)).get();
	}

	@Test
	public void thenTheMediaScannerReceivesTheCorrectFile() {
		assertThat(Stream.of(collectedFiles).map(File::getPath).toList()).containsExactly("test");
	}
}
