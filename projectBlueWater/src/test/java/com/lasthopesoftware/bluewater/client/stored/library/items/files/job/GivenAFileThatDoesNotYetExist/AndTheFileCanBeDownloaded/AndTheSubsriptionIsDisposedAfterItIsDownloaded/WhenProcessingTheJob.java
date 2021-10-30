package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItIsDownloaded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new LibraryId(13), 1, new ServiceFile(1), "test-path", true);
	private static final AccessStoredFiles storedFileAccess = mock(AccessStoredFiles.class);
	private static final List<StoredFileJobState> states = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200, new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			storedFileAccess,
			(libraryId, f) -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(new StoredFileJob(new LibraryId(13), new ServiceFile(1), storedFile)))
			.blockingSubscribe(new Observer<StoredFileJobStatus>() {
				private Disposable disposable;

				@Override
					public void onSubscribe(Disposable d) {
						this.disposable = d;
					}

					@Override
					public void onNext(StoredFileJobStatus status) {
						states.add(status.storedFileJobState);
						if (status.storedFileJobState == StoredFileJobState.Downloaded)
							disposable.dispose();
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
	public void thenTheFileIsMarkedAsDownloaded() {
		verify(storedFileAccess, times(1)).markStoredFileAsDownloaded(storedFile);
	}

	@Test
	public void thenTheJobStatesProgressCorrectly() {
		assertThat(states).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Downloaded);
	}
}
