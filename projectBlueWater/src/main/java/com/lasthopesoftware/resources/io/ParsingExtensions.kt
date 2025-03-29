package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateCancellableResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import kotlin.coroutines.cancellation.CancellationException

private fun xmlParsingCancelledException() = CancellationException("XML parsing was cancelled.")

fun Promise<HttpResponse>.promiseStringBody(): Promise<String> = then(HttpStringBodyResponse)
fun Promise<String>.promiseXmlDocument(): Promise<Document> = eventually(ParsedXmlDocumentResponse)
fun Promise<HttpResponse>.promiseStandardResponse(): Promise<StandardResponse> = HttpParsedStandardResponse(this)

object HttpStringBodyResponse : ImmediateCancellableResponse<HttpResponse, String> {
	override fun respond(response: HttpResponse, cancellationSignal: CancellationSignal): String = response.use {
		if (cancellationSignal.isCancelled) throw CancellationException("Reading body into string cancelled.")
		it.bodyString
	}
}

object ParsedXmlDocumentResponse : PromisedResponse<String, Document> {
	override fun promiseResponse(xmlString: String): Promise<Document> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		Jsoup.parse(xmlString, Parser.xmlParser())
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
