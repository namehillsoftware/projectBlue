package com.lasthopesoftware.resources.io

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.StringReader
import java.util.concurrent.CancellationException

inline fun <reified T> Gson.fromJson(value: String): T? = fromJson(value, T::class.java)

/**
 * Deserializes the string body and closes the response.
 */
fun Promise<HttpResponse>.promiseStringBody(): Promise<String> = eventually(HttpStringBodyResponse)
fun Promise<String>.promiseXmlDocument(): Promise<Document> = eventually(ParsedXmlDocumentResponse)
fun Promise<String>.promiseJson(): Promise<JsonElement?> = eventually(ParsedJsonDocumentResponse)
fun Promise<HttpResponse>.promiseStandardResponse(): Promise<StandardResponse> = HttpParsedStandardResponse(this)

object HttpStringBodyResponse : PromisedResponse<HttpResponse, String> {
	override fun promiseResponse(response: HttpResponse): Promise<String> = response.eventuallyUse { it.bodyString }
}

object ParsedXmlDocumentResponse : PromisedResponse<String, Document> {
	override fun promiseResponse(xmlString: String): Promise<Document> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		Jsoup.parse(xmlString, Parser.xmlParser())
	}
}

object ParsedJsonDocumentResponse : PromisedResponse<String, JsonElement?> {
	override fun promiseResponse(jsonString: String): Promise<JsonElement?> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw jsonParsingCancelledException()
		StringReader(jsonString).use { sr ->
			if (cs.isCancelled) throw jsonParsingCancelledException()
			JsonReader(sr).use { jr ->
				if (cs.isCancelled) throw jsonParsingCancelledException()
				JsonParser.parseReader(jr)
			}
		}
	}
}

class HttpParsedStandardResponse(sourcePromise: Promise<HttpResponse>) : Promise.Proxy<StandardResponse>() {
	init {
		proxy(
			sourcePromise
				.also(::doCancel)
				.promiseStringBody()
				.also(::doCancel)
				.promiseXmlDocument()
				.also(::doCancel)
				.eventually(ParsedStandardDocumentResponse)
		)
	}
}

object ParsedStandardDocumentResponse : PromisedResponse<Document, StandardResponse> {
	override fun promiseResponse(document: Document): Promise<StandardResponse> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		document.toStandardResponse()
	}
}

private fun xmlParsingCancelledException() = CancellationException("XML parsing was cancelled.")
private fun jsonParsingCancelledException() = CancellationException("JSON parsing was cancelled.")
