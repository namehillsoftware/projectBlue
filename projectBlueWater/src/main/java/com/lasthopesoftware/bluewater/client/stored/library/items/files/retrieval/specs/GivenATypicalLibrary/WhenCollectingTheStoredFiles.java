package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.specs.GivenATypicalLibrary;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCollectingTheStoredFiles extends AndroidContext {
	private static Collection<StoredFile> storedFiles;

	@Override
	public void before() throws Exception {
		try(final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(RuntimeEnvironment.application)) {
			final String insertSql = InsertBuilder
				.fromTable(StoredFileEntityInformation.tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.build();

			for (int i = 1; i < 10; i++) {
				repositoryAccessHelper
					.mapSql(insertSql)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, i)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, 2)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();
			}

			for (int i = 13; i < 23; i++) {
				repositoryAccessHelper
					.mapSql(insertSql)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, i)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, 5)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();
			}
		}

		final StoredFilesCollection storedFilesCollection = new StoredFilesCollection(RuntimeEnvironment.application);
		storedFiles = new FuturePromise<>(storedFilesCollection.promiseAllStoredFiles(new Library().setId(5))).get();
	}

	@Test
	public void thenTheStoredFilesAreFromTheCorrectLibrary() {
		assertThat(Stream.of(storedFiles).map(StoredFile::getLibraryId).toList()).containsOnly(5);
	}

	@Test
	public void thenTheStoredFilesAreCorrect() {
		assertThat(Stream.of(storedFiles).map(StoredFile::getServiceId).toList()).containsOnlyElementsOf(Stream.range(13, 23).toList());
	}
}
