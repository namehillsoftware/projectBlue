package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenASetOfStoredItems.AndSomeOfTheStoredItemsArePlaylists;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.ConvertStoredPlaylistsToStoredItems;
import com.lasthopesoftware.bluewater.client.library.items.stored.specs.FakeStoredItemAccess;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.annimon.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;
	private static List<ServiceFile> firstItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> secondItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> thirdItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> fourthItemExpectedFiles = givenARandomCollectionOfFiles();

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {

		final FakeStoredItemAccess storedItemAccess = new FakeStoredItemAccess(
			new StoredItem(1, 1, StoredItem.ItemType.ITEM),
			new StoredItem(1, 2, StoredItem.ItemType.ITEM),
			new StoredItem(1, 3, StoredItem.ItemType.ITEM),
			new StoredItem(1, 5, StoredItem.ItemType.PLAYLIST));

		final FileListParameters fileListParameters = FileListParameters.getInstance();
		final IFileProvider fileProvider = mock(IFileProvider.class);
		when(fileProvider.promiseFiles(any(), any()))
			.thenReturn(new Promise<>(Collections.emptyList()));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(1))))
			.thenReturn(new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(2))))
			.thenReturn(new Promise<>(secondItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(3))))
			.thenReturn(new Promise<>(thirdItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(12))))
			.thenReturn(new Promise<>(fourthItemExpectedFiles));

		final ConvertStoredPlaylistsToStoredItems storedPlaylistsToStoredItems = mock(ConvertStoredPlaylistsToStoredItems.class);
		when(storedPlaylistsToStoredItems.promiseConvertedStoredItem(argThat(a -> a.getItemType() == StoredItem.ItemType.PLAYLIST && a.getServiceId() == 5)))
			.thenReturn(new Promise<>(new StoredItem(1, 12, StoredItem.ItemType.ITEM)));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			storedPlaylistsToStoredItems,
			fileProvider);

		collectedFiles =
			new FuturePromise<>(serviceFileCollector
			.promiseServiceFilesToSync()).get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenAllTheServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(new HashSet<>(
			concat(
				concat(Stream.of(firstItemExpectedFiles), Stream.of(secondItemExpectedFiles)),
				concat(Stream.of(thirdItemExpectedFiles), Stream.of(fourthItemExpectedFiles))).toList()));
	}

	private static List<ServiceFile> givenARandomCollectionOfFiles() {
		final Random random = new Random();
		final int floor = random.nextInt(10000);
		final int ceiling = random.nextInt(10000 - floor) + floor;
		return Stream.range(floor, ceiling).map(ServiceFile::new).toList();
	}
}
