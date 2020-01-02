package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenASetOfStoredItems;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.ProvideLibraryFiles;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final IStoredItemAccess storedItemAccessMock = mock(IStoredItemAccess.class);
		when(storedItemAccessMock.promiseStoredItems(new LibraryId(52)))
			.thenReturn(new Promise<>(Collections.singleton(
				new StoredItem(52, 14, StoredItem.ItemType.PLAYLIST))));

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final ProvideLibraryFiles mockFileProvider = mock(ProvideLibraryFiles.class);
		when(mockFileProvider.promiseFiles(new LibraryId(52), FileListParameters.Options.None, fileListParameters.getFileListParameters(new Playlist(14))))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new StoredItemServiceFileCollector(
				storedItemAccessMock,
				mockFileProvider,
				fileListParameters),
			storedFileAccess,
			(l, sf) -> new Promise<>(new StoredFile(l, 1, sf, "fake-file-name", true)),
			jobs -> Observable.fromIterable(jobs).flatMap(job ->
				Observable.just(new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloading),
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloaded)))
		);

		storedFileJobResults = librarySyncHandler.observeLibrarySync(new LibraryId(52))
			.filter(j -> j.storedFileJobState == StoredFileJobState.Downloaded)
			.map(j -> j.storedFile)
			.toList()
			.blockingGet();
	}

	@Test
	public void thenTheFilesInTheStoredItemsAreSynced() {
		assertThat(Stream.of(storedFileJobResults).map(StoredFile::getServiceId).toList())
			.containsExactly(1, 2, 4, 10);
	}
}
