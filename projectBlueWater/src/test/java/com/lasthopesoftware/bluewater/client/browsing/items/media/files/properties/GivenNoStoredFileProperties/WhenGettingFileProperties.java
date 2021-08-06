package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.GivenNoStoredFileProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.RevisionChecker;
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class WhenGettingFileProperties {

	private static Map<String, String> fileProperties;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeFileConnectionProvider fakeFileConnectionProvider = new FakeFileConnectionProvider();
		fakeFileConnectionProvider.setupFile(new ServiceFile(15), new HashMap<String, String>() {
			{
				put(KnownFileProperties.KEY, "45");
			}
		});

		final FakeLibraryConnectionProvider fakeLibraryConnectionProvider = new FakeLibraryConnectionProvider(new HashMap<LibraryId, IConnectionProvider>() {{ put(new LibraryId(14), fakeFileConnectionProvider); }});

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(
			fakeLibraryConnectionProvider,
			new RevisionChecker(fakeLibraryConnectionProvider),
			mock(IFilePropertiesContainerRepository.class));

		fileProperties = new FuturePromise<>(filePropertiesProvider.promiseFileProperties(new LibraryId(14), new ServiceFile(15))).get();
	}

	@Test
	public void thenFilesAreRetrieved() {
		assertThat(fileProperties.get(KnownFileProperties.KEY)).isEqualTo("45");
	}
}
