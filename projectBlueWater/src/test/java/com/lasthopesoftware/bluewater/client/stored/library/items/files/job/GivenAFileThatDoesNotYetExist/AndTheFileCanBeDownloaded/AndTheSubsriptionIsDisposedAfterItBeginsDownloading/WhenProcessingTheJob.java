package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItBeginsDownloading;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new LibraryId(55), 1, new ServiceFile(1), "test-path", true);
	private static final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
	private static final List<StoredFileJobState> states = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final DeferredPromise<InputStream> deferredPromise = new DeferredPromise<>(new ByteArrayInputStream(new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			storedFileAccess,
			(libraryId, f) -> deferredPromise,
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(new StoredFileJob(new LibraryId(55), new ServiceFile(1), storedFile)))
			.blockingSubscribe(new Observer<StoredFileJobStatus>() {
				private Disposable disposable;

				@Override
					public void onSubscribe(Disposable d) {
						this.disposable = d;
					}

					@Override
					public void onNext(StoredFileJobStatus status) {
						states.add(status.storedFileJobState);

						if (status.storedFileJobState != StoredFileJobState.Downloading) return;

						disposable.dispose();
						deferredPromise.resolve();
					}

					@Override
					public void onError(Throwable e) {

					}

					@Override
					public void onComplete() {

					}
				});
	}

	@Test
	public void thenTheFileIsNotMarkedAsDownloaded() {
		verify(storedFileAccess, never()).markStoredFileAsDownloaded(storedFile);
	}

	@Test
	public void thenTheJobStatesProgressCorrectly() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading);
	}
}
