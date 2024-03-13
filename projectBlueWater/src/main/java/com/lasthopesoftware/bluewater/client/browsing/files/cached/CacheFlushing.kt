package com.lasthopesoftware.bluewater.client.browsing.files.cached

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.DiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.ProvideDiskCacheDirectory
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.CACHE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_SIZE
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.LAST_ACCESSED_TIME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.tableName
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.toListenableFuture
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

object CacheFlushing {

	private const val WORK_PREFIX = "flushCache"
	private const val CACHE_NAME_KEY = "cacheName"
	private val logger by lazyLogger<CacheFlushing>()

	fun promisedCacheFlushing(
		context: Context,
		diskCacheDirectory: ProvideDiskCacheDirectory,
		diskFileCacheConfiguration: DiskFileCacheConfiguration,
		targetSize: Long
	): Promise<*> =
		promiseTableMessage<Unit, CachedFile>(
			CacheFlusherTask(
				context,
				diskCacheDirectory,
				diskFileCacheConfiguration,
				targetSize
			)
		)

	fun scheduleCacheFlushing(context: Context, diskFileCacheConfiguration: DiskFileCacheConfiguration) {
		val cacheName = diskFileCacheConfiguration.cacheName
		val oneTimeWorkRequest = OneTimeWorkRequest.Builder(CacheFlushingWorker::class.java)
			.setConstraints(Constraints.Builder().setRequiresCharging(true).build())
			.setInputData(Data.Builder().putString(CACHE_NAME_KEY, cacheName).build())
			.build()

		WorkManager
			.getInstance(context)
			.enqueueUniqueWork("$WORK_PREFIX:$cacheName", ExistingWorkPolicy.APPEND_OR_REPLACE, oneTimeWorkRequest)
	}

	class CacheFlushingWorker(private val context: Context, private val workerParams: WorkerParameters) : ListenableWorker(context, workerParams)
	{
		private val cancellationProxy = CancellationProxy()

		override fun startWork(): ListenableFuture<Result> {
			val cacheConfiguration = when (workerParams.inputData.getString(CACHE_NAME_KEY)) {
				AudioCacheConfiguration.cacheName -> AudioCacheConfiguration
				ImageCacheConfiguration.cacheName -> ImageCacheConfiguration
				else -> return Promise(Result.success()).toListenableFuture()
			}

			val diskCacheDirectory = AndroidDiskCacheDirectoryProvider(context, cacheConfiguration)

			return promisedCacheFlushing(context, diskCacheDirectory, cacheConfiguration, cacheConfiguration.maxSize)
				.also(cancellationProxy::doCancel)
				.then { _ -> Result.success() }
				.toListenableFuture()
		}

		override fun onStopped() = cancellationProxy.cancellationRequested()
	}

	private class CacheFlusherTask(
		private val context: Context,
		private val diskCacheDirectory: ProvideDiskCacheDirectory,
		private val diskFileCacheConfiguration: DiskFileCacheConfiguration,
		private val targetSize: Long
	) : CancellableMessageWriter<Unit> {

		companion object {
			private fun RepositoryAccessHelper.getCachedFileByFilename(fileName: String): CachedFile? =
				mapSql("SELECT * FROM $tableName WHERE $FILE_NAME = @$FILE_NAME")
					.addParameter(FILE_NAME, fileName)
					.fetchFirst(CachedFile::class.java)

			private fun RepositoryAccessHelper.deleteCachedFile(cachedFile: CachedFile): Boolean =
				((cachedFile.fileName?.let(::File)?.run { exists() && delete() } ?: false)
					and (mapSql("DELETE FROM $tableName WHERE id = @id")
					.addParameter("id", cachedFile.id)
					.execute() > 0))
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			flushCache(cancellationSignal)
		}

		private fun flushCache(cancellationSignal: CancellationSignal) {
			RepositoryAccessHelper(context).use { repositoryAccess ->
				fun RepositoryAccessHelper.isCacheFull() = getCachedFileSizeFromDatabase() > targetSize

				if (cancellationSignal.isCancelled) return

				if (!repositoryAccess.isCacheFull()) return
				while (repositoryAccess.isCacheFull()) {
					if (cancellationSignal.isCancelled) return

					repositoryAccess.getOldestCachedFile()?.also { repositoryAccess.deleteCachedFile(it) }
				}

				if (cancellationSignal.isCancelled) return

				// Remove any files in the cache dir but not in the database
				val filesInCacheDir = diskCacheDirectory
					.getRootDiskCacheDirectory()
					?.takeIf { it.exists() }
					?.listFiles()
					?.flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() } ?: return

				if (cancellationSignal.isCancelled) return

				// If the # of files in the cache dir is equal to the database size, then
				// hypothetically (and good enough for our purposes), they are in sync and we don't need
				// to do additional processing
				if (filesInCacheDir.size.toLong() == repositoryAccess.getCachedFileCount()) return

				// Remove all files that aren't tracked in the database
				for (fileInCacheDir in filesInCacheDir) {
					if (cancellationSignal.isCancelled) return

					try {
						if (repositoryAccess.getCachedFileByFilename(fileInCacheDir.canonicalPath) != null) continue
					} catch (e: IOException) {
						logger.warn("Issue getting canonical file path.", e)
					}

					if (cancellationSignal.isCancelled) return

					if (fileInCacheDir.isDirectory) {
						try {
							FileUtils.deleteDirectory(fileInCacheDir)
						} catch (e: IOException) {
							logger.warn("The cache directory `${fileInCacheDir.path}` could not be deleted.", e)
						}
						continue
					}

					if (fileInCacheDir.delete()) continue

					logger.warn("The cached file `${fileInCacheDir.path}` could not be deleted.")
				}
			}
		}

		private fun RepositoryAccessHelper.getCachedFileSizeFromDatabase(): Long =
			mapSql("SELECT SUM($FILE_SIZE) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
				.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
				.execute()

		private fun RepositoryAccessHelper.getOldestCachedFile(): CachedFile? =
			mapSql("SELECT * FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME ORDER BY $LAST_ACCESSED_TIME ASC")
				.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
				.fetchFirst(CachedFile::class.java)

		private fun RepositoryAccessHelper.getCachedFileCount(): Long =
			mapSql("SELECT COUNT(*) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
				.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
				.execute()
	}
}
