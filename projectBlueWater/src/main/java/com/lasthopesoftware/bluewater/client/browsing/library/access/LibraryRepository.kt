package com.lasthopesoftware.bluewater.client.browsing.library.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.repository.insert
import com.lasthopesoftware.bluewater.repository.update
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class LibraryRepository(private val context: Context) : ILibraryStorage, ILibraryProvider {
	override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> =
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
						.fetchFirst()
				}
			}
		}
	}

	private class SaveLibraryWriter constructor(private val context: Context, private val library: Library) : MessageWriter<Library> {

		companion object {
			private val logger by lazyLogger<SaveLibraryWriter>()

		}

		override fun prepareMessage(): Library =
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					val isLibraryExists = library.id > -1

					val returnLibrary =
						if (isLibraryExists) repositoryAccessHelper.update(tableName, library)
						else repositoryAccessHelper.insert(tableName, library)
					logger.debug("Library saved.")
					closeableTransaction.setTransactionSuccessful()
					returnLibrary
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
