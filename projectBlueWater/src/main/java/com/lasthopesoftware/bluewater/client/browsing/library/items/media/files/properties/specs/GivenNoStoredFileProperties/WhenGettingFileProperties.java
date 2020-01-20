package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.specs.GivenNoStoredFileProperties;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeFileConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeLibraryConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(
			new FakeLibraryConnectionProvider(new HashMap<LibraryId, IConnectionProvider>() {{ put(new LibraryId(14), fakeFileConnectionProvider); }}),
			mock(IFilePropertiesContainerRepository.class));

		fileProperties = new FuturePromise<>(filePropertiesProvider.promiseFileProperties(new LibraryId(14), new ServiceFile(15))).get();
	}

	@Test
	public void thenFilesAreRetrieved() {
		assertThat(fileProperties.get(KnownFileProperties.KEY)).isEqualTo("45");
	}
}
