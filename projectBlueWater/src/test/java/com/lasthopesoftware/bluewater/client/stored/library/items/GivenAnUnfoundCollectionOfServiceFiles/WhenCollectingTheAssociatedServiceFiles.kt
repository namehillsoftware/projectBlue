package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnUnfoundCollectionOfServiceFiles

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import kotlin.random.Random.Default.nextInt

class WhenCollectingTheAssociatedServiceFiles {

	private val firstItemExpectedFiles by lazy { givenARandomCollectionOfFiles() }
	private val thirdItemExpectedFiles by lazy { givenARandomCollectionOfFiles() }
	private val syncToggledItems = HashMap<KeyedIdentifier, Boolean>()
	private val serviceFileCollector by lazy {
		val storedItemAccess: AccessStoredItems = object : FakeStoredItemAccess(
			StoredItem(4, 1, StoredItem.ItemType.ITEM),
			StoredItem(4, 2, StoredItem.ItemType.ITEM),
			StoredItem(4, 3, StoredItem.ItemType.ITEM)
		) {
			override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
				syncToggledItems[itemId] = enable
				return super.toggleSync(libraryId, itemId, enable)
			}
		}

		val fileListParameters = FileListParameters
		val fileProvider = mockk<ProvideLibraryFiles>()
		every {
			fileProvider.promiseFiles(
				LibraryId(4),
				FileListParameters.Options.None,
				*fileListParameters.getFileListParameters(ItemId(1))
			)
		} returns firstItemExpectedFiles.toPromise()

		every {
			fileProvider.promiseFiles(
				LibraryId(4),
				FileListParameters.Options.None,
				*fileListParameters.getFileListParameters(ItemId(2))
			)
		} returns Promise(FileNotFoundException())

		every {
			fileProvider.promiseFiles(
				LibraryId(4),
				FileListParameters.Options.None,
				*fileListParameters.getFileListParameters(ItemId(3))
			)
		} returns thirdItemExpectedFiles.toPromise()

		StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			fileListParameters
		)
	}

	private lateinit var collectedFiles: Collection<ServiceFile>

	@BeforeAll
	fun act() {
		collectedFiles = serviceFileCollector
			.promiseServiceFilesToSync(LibraryId(4))
			.toExpiringFuture().get()!!
	}

	@Test
	fun `then only the found service files are returned`() {
		assertThat(collectedFiles).hasSameElementsAs(firstItemExpectedFiles.plus(thirdItemExpectedFiles).toHashSet())
	}

	@Test
	fun `then the file that was not found had sync toggled off`() {
		assertThat(syncToggledItems).hasEntrySatisfying(object : Condition<KeyedIdentifier>() {
			override fun matches(value: KeyedIdentifier): Boolean = value.id == 2
		}, object : Condition<Boolean>() {
			override fun matches(value: Boolean): Boolean = !value
		})
	}

	private fun givenARandomCollectionOfFiles(): List<ServiceFile> {
		val floor = nextInt(10000)
		val ceiling = nextInt(10000 - floor) + floor
		return IntRange(floor, ceiling).map { ServiceFile(it) }.toList()
	}
}
