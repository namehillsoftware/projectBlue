package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItIsDownloaded;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
	private static List<StoredFileJobState> states = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200, new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			storedFileAccess,
			f -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(new StoredFileJob(new ServiceFile(1), storedFile)))
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
