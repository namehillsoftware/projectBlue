package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnUnfoundCollectionOfServiceFiles

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenCollectingTheAssociatedServiceFiles {

	companion object {
		private lateinit var collectedFiles: Collection<ServiceFile>
		private val firstItemExpectedFiles = givenARandomCollectionOfFiles()
		private val thirdItemExpectedFiles = givenARandomCollectionOfFiles()
		private val syncToggledItems = HashMap<IItem, Boolean>()

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val storedItemAccess: IStoredItemAccess = object : FakeStoredItemAccess(
				StoredItem(1, 1, StoredItem.ItemType.ITEM),
				StoredItem(1, 2, StoredItem.ItemType.ITEM),
				StoredItem(1, 3, StoredItem.ItemType.ITEM)
			) {
				override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {
					syncToggledItems[item] = enable
					super.toggleSync(libraryId, item, enable)
				}
			}
			val fileListParameters = FileListParameters.getInstance()
			val fileProvider = mockk<ProvideLibraryFiles>()
			every {
				fileProvider.promiseFiles(
					LibraryId(4),
					FileListParameters.Options.None,
					*fileListParameters.getFileListParameters(Item(1)))
			} returns firstItemExpectedFiles.toPromise()

			every {
				fileProvider.promiseFiles(
					LibraryId(4),
					FileListParameters.Options.None,
					*fileListParameters.getFileListParameters(Item(2))
				)
			} returns Promise(FileNotFoundException())

			every {
				fileProvider.promiseFiles(
					LibraryId(4),
					FileListParameters.Options.None,
					*fileListParameters.getFileListParameters(Item(3))
				)
			} returns thirdItemExpectedFiles.toPromise()

			val serviceFileCollector = StoredItemServiceFileCollector(
				storedItemAccess,
				fileProvider,
				fileListParameters
			)

			collectedFiles = serviceFileCollector
				.promiseServiceFilesToSync(LibraryId(4))
				.toFuture()[1000, TimeUnit.SECONDS]!!
		}

		private fun givenARandomCollectionOfFiles(): List<ServiceFile> {
			val random = Random()
			val floor = random.nextInt(10000)
			val ceiling = random.nextInt(10000 - floor) + floor
			return IntRange(floor, ceiling).map { ServiceFile(it) }.toList()
		}
	}

	@Test
	fun thenOnlyTheFoundServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(firstItemExpectedFiles.plus(thirdItemExpectedFiles).toHashSet())
	}

	@Test
	fun thenTheFileThatWasNotFoundHadSyncToggledOff() {
		assertThat(syncToggledItems).hasEntrySatisfying(object : Condition<IItem>() {
			override fun matches(value: IItem): Boolean = value.key == 2
		}, object : Condition<Boolean>() {
			override fun matches(value: Boolean): Boolean = !value
		})
	}
}
