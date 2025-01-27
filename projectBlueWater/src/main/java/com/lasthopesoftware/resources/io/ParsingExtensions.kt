package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateCancellableResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import okio.use
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import kotlin.coroutines.cancellation.CancellationException

private fun xmlParsingCancelledException() = CancellationException("XML parsing was cancelled.")

fun Promise<Response>.promiseStringBody(): Promise<String> = then(StringBodyResponse)
fun Promise<String>.promiseXmlDocument(): Promise<Document> = eventually(ParsedXmlDocumentResponse)
fun Promise<String>.promiseHtmlDocument(): Promise<Document> = eventually(ParsedHtmlDocumentResponse)
fun Promise<Response>.promiseStandardResponse(): Promise<StandardResponse> =
	promiseStringBody()
		.promiseXmlDocument()
		.eventually(ParsedStandardDocumentResponse)

object StringBodyResponse : ImmediateCancellableResponse<Response, String> {
	override fun respond(response: Response, cancellationSignal: CancellationSignal): String = response.use {
		if (cancellationSignal.isCancelled) throw CancellationException("Reading body into string cancelled.")
		it.body.string()
	}
}

object ParsedXmlDocumentResponse : PromisedResponse<String, Document> {
	override fun promiseResponse(xmlString: String): Promise<Document> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		Jsoup.parse(xmlString, Parser.xmlParser())
	}
}

object ParsedHtmlDocumentResponse : PromisedResponse<String, Document> {
	override fun promiseResponse(xmlString: String): Promise<Document> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		Jsoup.parse(xmlString)
	}
}

object ParsedStandardDocumentResponse : PromisedResponse<Document, StandardResponse> {
	override fun promiseResponse(document: Document): Promise<StandardResponse> = ThreadPools.compute.preparePromise { cs ->
		if (cs.isCancelled) throw xmlParsingCancelledException()

		document.toStandardResponse()
	}
}
