package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenACancellationWhileGettingStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.ProvideLibraryFiles;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.specs.FakeDeferredStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static IStoredFileAccess storedFileAccess;

	@BeforeClass
	public static void before() {
		final FakeDeferredStoredItemAccess deferredStoredItemAccess = new FakeDeferredStoredItemAccess() {
			@Override
			protected Collection<StoredItem> getStoredItems() {
				return Collections.singleton(new StoredItem(1, 14, StoredItem.ItemType.PLAYLIST));
			}
		};

		final ProvideLibraryFiles mockFileProvider = mock(ProvideLibraryFiles.class);
		when(mockFileProvider.promiseFiles(
			new LibraryId(13),
			FileListParameters.Options.None, "Playlist/Files", "Playlist=14"))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new StoredItemServiceFileCollector(
				deferredStoredItemAccess,
				mockFileProvider,
				FileListParameters.getInstance()),
			storedFileAccess,
			(l, f) -> new Promise<>(new StoredFile(new Library().setId(l.getId()), 1, f, "fake-file-name", true)),
			jobs -> Observable.fromIterable(jobs).flatMap(job -> Observable.just(
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloading),
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloaded))));

		final Observable<StoredFile> syncedFiles = librarySyncHandler.observeLibrarySync(new LibraryId(5)).map(j -> j.storedFile);

		syncedFiles.blockingSubscribe(new Observer<StoredFile>() {
			@Override
			public void onSubscribe(Disposable d) {
				d.dispose();
			}

			@Override
			public void onNext(StoredFile storedFile) {
				storedFileJobResults.add(storedFile);
			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onComplete() {

			}
		});

		deferredStoredItemAccess.resolveStoredItems();
	}

	@Test
	public void thenTheFilesInTheStoredItemsAreNotSynced() {
		assertThat(storedFileJobResults).isEmpty();
	}

	@Test
	public void thenFilesAreNotPruned() {
		verify(storedFileAccess, never()).pruneStoredFiles(any(), anySet());
	}
}
