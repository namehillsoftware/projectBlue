package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.broadcasts.ScanMediaFileBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.io.FileStreamWriter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.DelegatingStoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredFilesCounter
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesChecker
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.CachingSyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.*
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

abstract class SyncWorker(private val context: Context, workerParams: WorkerParameters) :
	ListenableWorker(context, workerParams),
	PostSyncNotification
{
	private val syncChecker by lazy {
		SyncChecker(
			LibraryRepository(context),
			serviceFilesCollector,
			StoredFilesChecker(StoredFilesCounter(StoredFilesCollection(context)))
		)
	}

	private val applicationSettings by lazy { context.getApplicationSettingsRepository() }

	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(context)) }
	private val storedFileAccess by lazy { StoredFileAccess(context) }
	private val readPermissionArbitratorForOs by lazy { ExternalStorageReadPermissionsArbitratorForOs(context) }
	private val libraryRepository by lazy { LibraryRepository(context) }
	private val libraryIdentifierProvider by lazy { SelectedBrowserLibraryIdentifierProvider(applicationSettings) }
	private val libraryConnections by lazy { ConnectionSessionManager.get(context) }

	private val cachingPolicyFactory by lazy { CachingPolicyFactory() }

	private val fileProperties by lazy {
		val filePropertyCache = FilePropertyCache.getInstance()
		CachedFilePropertiesProvider(
			libraryConnections,
			filePropertyCache,
			FilePropertiesProvider(
				libraryConnections,
				LibraryRevisionProvider(libraryConnections),
				filePropertyCache
			)
		)
	}

	private val serviceFilesCollector by lazy {
		val serviceFilesCollector = StoredItemServiceFileCollector(
			StoredItemAccess(context),
			LibraryFileProvider(LibraryFileStringListProvider(libraryConnections)),
			FileListParameters.getInstance())

		DelegatingStoredItemServiceFileCollector(
			serviceFilesCollector,
			cachingPolicyFactory
		)
	}

	private val storedFilesPruner by lazy {
		StoredFilesPruner(
			serviceFilesCollector,
			StoredFilesCollection(context),
			storedFileAccess
		)
	}

	private val storedFilesSynchronization by lazy {
		val cursorProvider = MediaQueryCursorProvider(
			context,
			fileProperties
		)

		val storedFileUpdater = StoredFileUpdater(
			context,
			MediaFileUriProvider(
				context,
				cursorProvider,
				readPermissionArbitratorForOs,
				libraryIdentifierProvider,
				true
			),
			MediaFileIdProvider(
				cursorProvider,
				readPermissionArbitratorForOs
			),
			StoredFileQuery(context),
			libraryRepository,
			fileProperties,
			CachingSyncDirectoryLookup(
				SyncDirectoryLookup(libraryRepository, PublicDirectoryLookup(context), PrivateDirectoryLookup(context), FreeSpaceLookup),
				cachingPolicyFactory
			)
		)

		val syncHandler = LibrarySyncsHandler(
			serviceFilesCollector,
			storedFilesPruner,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileSystemFileProducer(),
				storedFileAccess,
				StoredFileDownloader(ServiceFileUriQueryParamsProvider(), libraryConnections),
				FileReadPossibleArbitrator(),
				FileWritePossibleArbitrator(),
				FileStreamWriter()
			)
		)
		StoredFileSynchronization(
			libraryRepository,
			messageBus,
			storedFilesPruner,
			syncChecker,
			syncHandler
		)
	}

	private val lazyStoredFileEventReceivers = lazy {
		val storedFileDownloadingNotifier = StoredFileDownloadingNotifier(
			storedFileAccess,
			fileProperties,
			this,
			context)
		val storedFileMediaScannerNotifier =
			StoredFileMediaScannerNotifier(
				storedFileAccess,
				ScanMediaFileBroadcaster(context)
			)
		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			readPermissionArbitratorForOs,
			StorageReadPermissionsRequestedBroadcaster(messageBus),
			storedFileAccess)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			ExternalStorageWritePermissionsArbitratorForOs(context),
			StorageWritePermissionsRequestedBroadcaster(messageBus),
			storedFileAccess)

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileMediaScannerNotifier,
			storedFileReadPermissionsReceiver,
			storedFileWritePermissionsReceiver)
	}

	private val syncStartedReceiver = lazy { SyncStartedReceiver(this) }

	private val cancellationProxy = CancellationProxy()

	override fun startWork(): ListenableFuture<Result> {
		val futureResult = SettableFuture.create<Result>()

		doWork().then({ futureResult.set(Result.success()) }, futureResult::setException)

		return futureResult
	}

	override fun onStopped() = cancellationProxy.run()

	private fun doWork(): Promise<Unit> {
		if (cancellationProxy.isCancelled) return Unit.toPromise()

		if (!lazyStoredFileEventReceivers.isInitialized()) {
			for (receiveStoredFileEvent in lazyStoredFileEventReceivers.value.distinct()) {
				val broadcastReceiver = StoredFileBroadcastReceiver(receiveStoredFileEvent)
				messageBus.registerReceiver(
					broadcastReceiver,
					receiveStoredFileEvent.acceptedEvents().fold(IntentFilter(), { i, e ->
						i.addAction(e)
						i
					}))
			}
		}

		if (!syncStartedReceiver.isInitialized()) {
			messageBus.registerReceiver(
				syncStartedReceiver.value,
				syncStartedReceiver.value.acceptedEvents().fold(IntentFilter(), { i, e ->
					i.addAction(e)
					i
				}))
		}

		return if (cancellationProxy.isCancelled) Unit.toPromise()
		else storedFilesSynchronization.streamFileSynchronization()
			.toPromise()
			.also(cancellationProxy::doCancel)
			.inevitably { continueWork(cancellationProxy) }
			.must { messageBus.clear() }
	}

	abstract fun continueWork(cancellationProxy: CancellationProxy): Promise<Unit>
}
