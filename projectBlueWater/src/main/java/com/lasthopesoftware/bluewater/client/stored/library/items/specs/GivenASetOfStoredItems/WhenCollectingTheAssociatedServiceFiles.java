package com.lasthopesoftware.bluewater.client.stored.library.items.specs.GivenASetOfStoredItems;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.ProvideFiles;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.annimon.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;
	private static List<ServiceFile> firstItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> secondItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> thirdItemExpectedFiles = givenARandomCollectionOfFiles();

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {

		final IStoredItemAccess storedItemAccess = mock(IStoredItemAccess.class);
		when(storedItemAccess.promiseStoredItems(new LibraryId(15)))
			.thenReturn(new Promise<>(Arrays.asList(
				new StoredItem(1, 1, StoredItem.ItemType.ITEM),
				new StoredItem(1, 2, StoredItem.ItemType.ITEM),
				new StoredItem(1, 3, StoredItem.ItemType.ITEM))));

		final FileListParameters fileListParameters = FileListParameters.getInstance();
		final ProvideFiles fileProvider = mock(ProvideFiles.class);
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(1))))
			.thenAnswer(e -> new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(2))))
			.thenAnswer(e -> new Promise<>(secondItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(3))))
			.thenAnswer(e -> new Promise<>(thirdItemExpectedFiles));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			fileListParameters);

		collectedFiles =
			new FuturePromise<>(serviceFileCollector
			.promiseServiceFilesToSync(new LibraryId(15))).get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenAllTheServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(new HashSet<>(concat(concat(Stream.of(firstItemExpectedFiles), Stream.of(secondItemExpectedFiles)), Stream.of(thirdItemExpectedFiles)).toList()));
	}

	private static List<ServiceFile> givenARandomCollectionOfFiles() {
		final Random random = new Random();
		final int floor = random.nextInt(10000);
		final int ceiling = random.nextInt(10000 - floor) + floor;
		return Stream.range(floor, ceiling).map(ServiceFile::new).toList();
	}
}
