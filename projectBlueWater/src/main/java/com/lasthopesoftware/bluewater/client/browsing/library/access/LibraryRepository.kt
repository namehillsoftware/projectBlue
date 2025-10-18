package com.lasthopesoftware.bluewater.client.browsing.library.access

import android.content.Context
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isRepeatingColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingIdColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingProgressColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.savedTracksStringColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirstOrNull
import com.lasthopesoftware.bluewater.repository.insert
import com.lasthopesoftware.bluewater.repository.update
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.querydroid.SqLiteAssistants
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class LibraryRepository(private val context: Context) : ManageLibraries, ProvideLibraries {
	override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> =
		promiseTableMessage(GetLibraryWriter(context, libraryId))

	override fun promiseAllLibraries(): Promise<Collection<Library>> =
		promiseTableMessage(GetAllLibrariesWriter(context))

	override fun saveLibrary(library: Library): Promise<Library> =
		promiseTableMessage(SaveLibraryWriter(context, library))

	override fun updateNowPlaying(values: LibraryNowPlayingValues): Promise<Unit> =
		promiseTableMessage(UpdateNowPlayingLibraryWriter(context, values))

	override fun promiseNowPlayingValues(libraryId: LibraryId): Promise<LibraryNowPlayingValues?> =
		promiseTableMessage(GetNowPlayingValuesWriter(context, libraryId))

	override fun removeLibrary(libraryId: LibraryId): Promise<Unit> =
		promiseTableMessage(RemoveLibraryWriter(context, libraryId))

	private class GetAllLibrariesWriter(private val context: Context) : CancellableMessageWriter<Collection<Library>> {
		override fun prepareMessage(cancellationSignal: CancellationSignal): Collection<Library> {
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting libraries")
			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM $tableName")
						.fetch()
				}
			}
		}
	}

	private class GetLibraryWriter(private val context: Context, private val libraryId: LibraryId) : CancellableMessageWriter<Library?> {

		override fun prepareMessage(cancellationSignal: CancellationSignal): Library? {
			val libraryInt = libraryId.id
			if (libraryInt < 0) return null
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting library.")

			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM $tableName WHERE id = @id")
						.addParameter("id", libraryInt)
						.fetchFirstOrNull()
				}
			}
		}
	}

	private class GetNowPlayingValuesWriter(private val context: Context, private val libraryId: LibraryId) : CancellableMessageWriter<LibraryNowPlayingValues?> {

		override fun prepareMessage(cancellationSignal: CancellationSignal): LibraryNowPlayingValues? {
			val libraryInt = libraryId.id
			if (libraryInt < 0) return null
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting now playing values.")

			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT id, $nowPlayingIdColumn, $nowPlayingProgressColumn, $savedTracksStringColumn, $isRepeatingColumn FROM $tableName WHERE id = @id")
						.addParameter("id", libraryInt)
						.fetchFirstOrNull()
				}
			}
		}
	}

	private class SaveLibraryWriter(private val context: Context, private val library: Library) : CancellableMessageWriter<Library> {

		companion object {
			private val logger by lazyLogger<SaveLibraryWriter>()

		}

		override fun prepareMessage(cancellationSignal: CancellationSignal): Library {
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while saving library")

			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				val isLibraryExists = library.id > -1

				val returnLibrary =
					if (isLibraryExists) repositoryAccessHelper.update(tableName, library)
					else repositoryAccessHelper.insert(tableName, library)

				if (BuildConfig.DEBUG) {
					logger.debug("Library saved.")
				}

				returnLibrary
			}
		}
	}

	private class UpdateNowPlayingLibraryWriter(
		private val context: Context,
		private val values: LibraryNowPlayingValues
	) : CancellableMessageWriter<Unit> {

		companion object {
			private val logger by lazyLogger<SaveLibraryWriter>()
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while saving now playing values.")

			val libraryInt = values.id
			if (libraryInt < 0) return

			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				val result = SqLiteAssistants.updateValue(repositoryAccessHelper.writableDatabase, tableName, values)

				if (result == 0L) {
					throw IOException("Updating $tableName for id ${values.id} returned 0 rows.")
				}

				if (BuildConfig.DEBUG) {
					logger.debug("Now Playing updated for library {}.", libraryInt)
				}
			}
		}
	}

	private class RemoveLibraryWriter(private val context: Context, private val libraryId: LibraryId) : CancellableMessageWriter<Unit> {

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			val libraryInt = libraryId.id
			if (libraryInt < 0 || cancellationSignal.isCancelled) return
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
