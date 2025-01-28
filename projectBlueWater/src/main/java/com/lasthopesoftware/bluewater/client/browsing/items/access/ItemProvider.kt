package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.io.promiseXmlDocument
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val browseLibraryParameter = "Browse/Children"

class ItemProvider(private val connectionProvider: ProvideGuaranteedLibraryConnections) :
	ProvideItems,
	ProvideFreshItems,
	PromisedResponse<Document, List<Item>>
{
    override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<Item>> = Promise.Proxy { cp ->
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.eventually { connectionProvider ->
				connectionProvider
					?.run {
						itemId
							?.run {
								promiseResponse(
									browseLibraryParameter,
									"ID=$id",
									"Version=2",
									"ErrorOnMissing=1"
								)
							}
							?: promiseResponse(browseLibraryParameter, "Version=2", "ErrorOnMissing=1")
					}
					?.also(cp::doCancel)
					?.promiseStringBody()
					?.also(cp::doCancel)
					?.promiseXmlDocument()
					?.also(cp::doCancel)
					?.eventually(this)
					.keepPromise(emptyList())
				}
	}

	override fun promiseResponse(document: Document): Promise<List<Item>> = ThreadPools.compute.preparePromise { cs ->
		val body = document.getElementsByTag("Response").firstOrNull()
			?: throw IOException("Response tag not found")

		val status = body.attr("Status")
		if (status.equals("Failure", ignoreCase = true))
			throw IOException("Server returned 'Failure'.")

		body
			.getElementsByTag("Item")
			.map { el ->
				if (cs.isCancelled) throw itemParsingCancelledException()
				Item(el.wholeOwnText().toInt(), el.attr("Name"))
			}
	}

	private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
}
