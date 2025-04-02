package com.lasthopesoftware.bluewater.client.browsing.library.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isRepeatingColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingIdColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingProgressColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.savedTracksStringColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.repository.insert
import com.lasthopesoftware.bluewater.repository.update
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.querydroid.SqLiteAssistants
import kotlin.coroutines.cancellation.CancellationException

class LibraryRepository(private val context: Context) : ILibraryStorage, ILibraryProvider {
	override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> =
		promiseTableMessage<Library?, Library>(GetLibraryWriter(context, libraryId))

	override fun promiseAllLibraries(): Promise<Collection<Library>> =
		promiseTableMessage<Collection<Library>, Library>(GetAllLibrariesWriter(context))

	override fun saveLibrary(library: Library): Promise<Library> =
		promiseTableMessage<Library, Library>(SaveLibraryWriter(context, library))

	override fun updateNowPlaying(
		libraryId: LibraryId,
		nowPlayingId: Int,
		nowPlayingProgress: Long,
		savedTracksString: String,
		isRepeating: Boolean
	): Promise<Unit> = promiseTableMessage<Unit, Library>(
		UpdateNowPlayingLibraryWriter(
			context,
			libraryId,
			nowPlayingId,
			nowPlayingProgress,
			savedTracksString,
			isRepeating)
	)

	override fun removeLibrary(libraryId: LibraryId): Promise<Unit> =
		promiseTableMessage<Unit, Library>(RemoveLibraryWriter(context, libraryId))

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
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting library")

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

	private class SaveLibraryWriter(private val context: Context, private val library: Library) : CancellableMessageWriter<Library> {

		companion object {
			private val logger by lazyLogger<SaveLibraryWriter>()

		}

		override fun prepareMessage(cancellationSignal: CancellationSignal): Library {
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while saving library")

			return RepositoryAccessHelper(context).use { repositoryAccessHelper ->
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
	}

	private class UpdateNowPlayingLibraryWriter(
		private val context: Context,
		private val libraryId: LibraryId,
		private val nowPlayingId: Int,
		private val nowPlayingProgress: Long,
		private val savedTracksString: String,
		private val isRepeating: Boolean
	) : CancellableMessageWriter<Unit> {

		companion object {
			private val logger by lazyLogger<SaveLibraryWriter>()

			private val updateStatement by lazy {
				SqLiteAssistants.UpdateBuilder
					.fromTable(tableName)
					.addSetter(nowPlayingIdColumn)
					.addSetter(nowPlayingProgressColumn)
					.addSetter(savedTracksStringColumn)
					.addSetter(isRepeatingColumn)
					.setFilter("where id = @id")
					.buildQuery()
			}
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while saving library")

			val libraryInt = libraryId.id
			if (libraryInt < 0) return

			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(updateStatement)
						.addParameter(nowPlayingIdColumn, nowPlayingId)
						.addParameter(nowPlayingProgressColumn, nowPlayingProgress)
						.addParameter(savedTracksStringColumn, savedTracksString)
						.addParameter(isRepeatingColumn, isRepeating)
						.addParameter("id", libraryInt)
						.execute()

					logger.debug("Now Playing updated for library $libraryInt.")
					closeableTransaction.setTransactionSuccessful()
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
