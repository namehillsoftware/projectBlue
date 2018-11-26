package com.lasthopesoftware.bluewater.client.library.access;

import android.content.Context;
import android.database.SQLException;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.namehillsoftware.artful.Artful;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class LibraryRepository implements ILibraryStorage, ILibraryProvider {
	private final Context context;

	public LibraryRepository(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Library> getLibrary(int libraryId) {
		return new QueuedPromise<>(new GetLibraryWriter(context, libraryId), RepositoryAccessHelper.databaseExecutor);
	}

	@Override
	public Promise<Collection<Library>> getAllLibraries() {
		return new QueuedPromise<>(new GetAllLibrariesWriter(context), RepositoryAccessHelper.databaseExecutor);
	}

	@Override
	public Promise<Library> saveLibrary(Library library) {
		return new QueuedPromise<>(new SaveLibraryWriter(context, library), RepositoryAccessHelper.databaseExecutor);
	}

	private static class GetAllLibrariesWriter implements MessageWriter<Collection<Library>> {

		private Context context;

		private GetAllLibrariesWriter(Context context) {
			this.context = context;
		}

		@Override
		public Collection<Library> prepareMessage() {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + Library.tableName)
						.fetch(Library.class);
			}
		}
	}

	private static class GetLibraryWriter implements MessageWriter<Library> {

		private int libraryId;
		private Context context;

		private GetLibraryWriter(Context context, int libraryId) {
			this.libraryId = libraryId;
			this.context = context;
		}

		@Override
		public Library prepareMessage() {
			if (libraryId < 0) return null;

			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = @id")
						.addParameter("id", libraryId)
						.fetchFirst(Library.class);
			}
		}
	}

	private static class SaveLibraryWriter implements MessageWriter<Library> {

		private static final Logger logger = LoggerFactory.getLogger(SaveLibraryWriter.class);

		private static final Lazy<String> libraryInsertSql
			= new Lazy<>(() ->
				InsertBuilder
					.fromTable(Library.tableName)
					.addColumn(Library.accessCodeColumn)
					.addColumn(Library.authKeyColumn)
					.addColumn(Library.isLocalOnlyColumn)
					.addColumn(Library.libraryNameColumn)
					.addColumn(Library.isRepeatingColumn)
					.addColumn(Library.customSyncedFilesPathColumn)
					.addColumn(Library.isSyncLocalConnectionsOnlyColumn)
					.addColumn(Library.isUsingExistingFilesColumn)
					.addColumn(Library.nowPlayingIdColumn)
					.addColumn(Library.nowPlayingProgressColumn)
					.addColumn(Library.savedTracksStringColumn)
					.addColumn(Library.selectedViewColumn)
					.addColumn(Library.selectedViewTypeColumn)
					.addColumn(Library.syncedFileLocationColumn)
					.build());

		private static final Lazy<String> libraryUpdateSql
			= new Lazy<>(() ->
				UpdateBuilder
					.fromTable(Library.tableName)
					.addSetter(Library.accessCodeColumn)
					.addSetter(Library.authKeyColumn)
					.addSetter(Library.isLocalOnlyColumn)
					.addSetter(Library.libraryNameColumn)
					.addSetter(Library.isRepeatingColumn)
					.addSetter(Library.customSyncedFilesPathColumn)
					.addSetter(Library.isSyncLocalConnectionsOnlyColumn)
					.addSetter(Library.isUsingExistingFilesColumn)
					.addSetter(Library.nowPlayingIdColumn)
					.addSetter(Library.nowPlayingProgressColumn)
					.addSetter(Library.savedTracksStringColumn)
					.addSetter(Library.selectedViewColumn)
					.addSetter(Library.selectedViewTypeColumn)
					.addSetter(Library.syncedFileLocationColumn)
					.setFilter("WHERE id = @id")
					.buildQuery());


		private Context context;
		private Library library;

		private SaveLibraryWriter(Context context, Library library) {
			this.context = context;
			this.library = library;
		}

		@Override
		public Library prepareMessage() {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					final boolean isLibraryExists = library.getId() > -1;

					final Artful artful =
						repositoryAccessHelper
							.mapSql(isLibraryExists ? libraryUpdateSql.getObject() : libraryInsertSql.getObject())
							.addParameter(Library.accessCodeColumn, library.getAccessCode())
							.addParameter(Library.authKeyColumn, library.getAuthKey())
							.addParameter(Library.isLocalOnlyColumn, library.isLocalOnly())
							.addParameter(Library.libraryNameColumn, library.getLibraryName())
							.addParameter(Library.isRepeatingColumn, library.isRepeating())
							.addParameter(Library.customSyncedFilesPathColumn, library.getCustomSyncedFilesPath())
							.addParameter(Library.isSyncLocalConnectionsOnlyColumn, library.isSyncLocalConnectionsOnly())
							.addParameter(Library.isUsingExistingFilesColumn, library.isUsingExistingFiles())
							.addParameter(Library.nowPlayingIdColumn, library.getNowPlayingId())
							.addParameter(Library.nowPlayingProgressColumn, library.getNowPlayingProgress())
							.addParameter(Library.savedTracksStringColumn, library.getSavedTracksString())
							.addParameter(Library.selectedViewColumn, library.getSelectedView())
							.addParameter(Library.selectedViewTypeColumn, library.getSelectedViewType())
							.addParameter(Library.syncedFileLocationColumn, library.getSyncedFileLocation());

					if (isLibraryExists)
						artful.addParameter("id", library.getId());

					final long result = artful.execute();
					closeableTransaction.setTransactionSuccessful();

					if (!isLibraryExists)
						library.setId((int) result);

					logger.debug("Library saved.");
					return library;
				} catch (SQLException se) {
					logger.error("There was an error saving the library", se);
					return null;
				}
			}
		}
	}
}
