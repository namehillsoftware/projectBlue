package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val browseLibraryParameter = "Browse/Children"

class ItemProvider(private val connectionProvider: ProvideGuaranteedLibraryConnections) :
	ProvideItems,
	ProvideFreshItems,
	PromisedResponse<Response, List<Item>>
{
    override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<Item>> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					.run {
						itemId
							?.run { promiseResponse(browseLibraryParameter, "ID=$id", "Version=2", "ErrorOnMissing=1") }
							?: promiseResponse(browseLibraryParameter, "Version=2", "ErrorOnMissing=1")
					}
			}
			.eventually(this)

	override fun promiseResponse(response: Response): Promise<List<Item>> = ThreadPools.compute.preparePromise { cs ->
		val bodyString = response.body.use { it.string() }
		if (cs.isCancelled) throw itemParsingCancelledException()

		val xml = Jsoup.parse(bodyString, Parser.xmlParser())

		val body = xml.getElementsByTag("Response").firstOrNull()
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
