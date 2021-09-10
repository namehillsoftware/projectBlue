package com.lasthopesoftware.bluewater.client.browsing.library.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository.SaveLibraryWriter
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import org.slf4j.LoggerFactory

class LibraryRepository(private val context: Context) : ILibraryStorage, ILibraryProvider {
	override fun getLibrary(libraryId: LibraryId): Promise<Library?> =
		DatabasePromise(GetLibraryWriter(context, libraryId))

	override val allLibraries: Promise<Collection<Library>>
		get() = DatabasePromise(GetAllLibrariesWriter(context))

	override fun saveLibrary(library: Library): Promise<Library> =
		DatabasePromise(SaveLibraryWriter(context, library))

	override fun removeLibrary(library: Library): Promise<Unit> =
		DatabasePromise(RemoveLibraryWriter(context, library))

	private class GetAllLibrariesWriter constructor(private val context: Context) : MessageWriter<Collection<Library>> {
		override fun prepareMessage(): Collection<Library> =
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM $tableName")
						.fetch(Library::class.java)
				}
			}
	}

	private class GetLibraryWriter constructor(private val context: Context, private val libraryId: LibraryId) : MessageWriter<Library?> {

		override fun prepareMessage(): Library? {
			val libraryInt = libraryId.id
			if (libraryInt < 0) return null
			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM $tableName WHERE id = @id")
						.addParameter("id", libraryInt)
						.fetchFirst(Library::class.java)
				}
			}
		}
	}

	private class SaveLibraryWriter constructor(private val context: Context, private val library: Library) : MessageWriter<Library> {

		companion object {
			private val logger = LoggerFactory.getLogger(SaveLibraryWriter::class.java)
			private val libraryInsertSql = lazy {
				fromTable(tableName)
					.addColumn(LibraryEntityInformation.accessCodeColumn)
					.addColumn(LibraryEntityInformation.userNameColumn)
					.addColumn(LibraryEntityInformation.passwordColumn)
					.addColumn(LibraryEntityInformation.isLocalOnlyColumn)
					.addColumn(LibraryEntityInformation.libraryNameColumn)
					.addColumn(LibraryEntityInformation.isRepeatingColumn)
					.addColumn(LibraryEntityInformation.isWakeOnLanEnabledColumn)
					.addColumn(LibraryEntityInformation.customSyncedFilesPathColumn)
					.addColumn(LibraryEntityInformation.isSyncLocalConnectionsOnlyColumn)
					.addColumn(LibraryEntityInformation.isUsingExistingFilesColumn)
					.addColumn(LibraryEntityInformation.nowPlayingIdColumn)
					.addColumn(LibraryEntityInformation.nowPlayingProgressColumn)
					.addColumn(LibraryEntityInformation.savedTracksStringColumn)
					.addColumn(LibraryEntityInformation.selectedViewColumn)
					.addColumn(LibraryEntityInformation.selectedViewTypeColumn)
					.addColumn(LibraryEntityInformation.syncedFileLocationColumn)
					.build()
			}

			private val libraryUpdateSql = lazy {
				UpdateBuilder
					.fromTable(tableName)
					.addSetter(LibraryEntityInformation.accessCodeColumn)
					.addSetter(LibraryEntityInformation.userNameColumn)
					.addSetter(LibraryEntityInformation.passwordColumn)
					.addSetter(LibraryEntityInformation.isLocalOnlyColumn)
					.addSetter(LibraryEntityInformation.libraryNameColumn)
					.addSetter(LibraryEntityInformation.isRepeatingColumn)
					.addSetter(LibraryEntityInformation.isWakeOnLanEnabledColumn)
					.addSetter(LibraryEntityInformation.customSyncedFilesPathColumn)
					.addSetter(LibraryEntityInformation.isSyncLocalConnectionsOnlyColumn)
					.addSetter(LibraryEntityInformation.isUsingExistingFilesColumn)
					.addSetter(LibraryEntityInformation.nowPlayingIdColumn)
					.addSetter(LibraryEntityInformation.nowPlayingProgressColumn)
					.addSetter(LibraryEntityInformation.savedTracksStringColumn)
					.addSetter(LibraryEntityInformation.selectedViewColumn)
					.addSetter(LibraryEntityInformation.selectedViewTypeColumn)
					.addSetter(LibraryEntityInformation.syncedFileLocationColumn)
					.setFilter("WHERE id = @id")
					.buildQuery()
			}
		}

		override fun prepareMessage(): Library =
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					val isLibraryExists = library.id > -1
					val artful = repositoryAccessHelper
						.mapSql(if (isLibraryExists) libraryUpdateSql.value else libraryInsertSql.value)
						.addParameter(LibraryEntityInformation.accessCodeColumn, library.accessCode)
						.addParameter(LibraryEntityInformation.userNameColumn, library.userName)
						.addParameter(LibraryEntityInformation.passwordColumn, library.password)
						.addParameter(LibraryEntityInformation.isLocalOnlyColumn, library.isLocalOnly)
						.addParameter(LibraryEntityInformation.libraryNameColumn, library.libraryName)
						.addParameter(LibraryEntityInformation.isRepeatingColumn, library.isRepeating)
						.addParameter(LibraryEntityInformation.isWakeOnLanEnabledColumn, library.isWakeOnLanEnabled)
						.addParameter(LibraryEntityInformation.customSyncedFilesPathColumn, library.customSyncedFilesPath)
						.addParameter(LibraryEntityInformation.isSyncLocalConnectionsOnlyColumn, library.isSyncLocalConnectionsOnly)
						.addParameter(LibraryEntityInformation.isUsingExistingFilesColumn, library.isUsingExistingFiles)
						.addParameter(LibraryEntityInformation.nowPlayingIdColumn, library.nowPlayingId)
						.addParameter(LibraryEntityInformation.nowPlayingProgressColumn, library.nowPlayingProgress)
						.addParameter(LibraryEntityInformation.savedTracksStringColumn, library.savedTracksString)
						.addParameter(LibraryEntityInformation.selectedViewColumn, library.selectedView)
						.addParameter(LibraryEntityInformation.selectedViewTypeColumn, library.selectedViewType)
						.addParameter(LibraryEntityInformation.syncedFileLocationColumn, library.syncedFileLocation)
					if (isLibraryExists) artful.addParameter("id", library.id)
					val result = artful.execute()
					closeableTransaction.setTransactionSuccessful()
					if (!isLibraryExists) library.setId(result.toInt())
					logger.debug("Library saved.")
					library
				}
			}
	}

	private class RemoveLibraryWriter constructor(private val context: Context, private val library: Library) : MessageWriter<Unit> {

		override fun prepareMessage() {
			val libraryInt = library.id
			if (libraryInt < 0) return
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use {
					repositoryAccessHelper
						.mapSql("DELETE FROM $tableName WHERE id = @id")
						.addParameter("id", libraryInt)
						.execute()
					it.setTransactionSuccessful()
				}
			}
		}
	}
}
