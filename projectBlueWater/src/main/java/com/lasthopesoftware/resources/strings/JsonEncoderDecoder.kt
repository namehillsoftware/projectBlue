package com.lasthopesoftware.resources.strings

import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.StringReader
import java.util.concurrent.CancellationException

object JsonEncoderDecoder : TranslateJson {
	private val jackson by lazy {
		jsonMapper {
			if (DebugFlag.isDebugCompilation) {
				enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
			}

			addModule(kotlinModule())
			enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
		}
	}

	override fun <T> parseJson(jsonElement: JsonNode, cls: Class<T>): T? =
		jackson.treeToValue(jsonElement, cls)

	override fun <T> parseJson(json: String, cls: Class<T>): T? =
		jackson.readValue(json, cls)

	override fun <T> toJson(obj: T): String =
		jackson.writeValueAsString(obj) ?: ""

	override fun Promise<String>.promiseJson(): Promise<JsonNode?> = eventually(ParsedJsonDocumentResponse)

	private object ParsedJsonDocumentResponse : PromisedResponse<String, JsonNode?> {
		override fun promiseResponse(jsonString: String): Promise<JsonNode?> = ThreadPools.compute.preparePromise { cs ->
			if (cs.isCancelled) throw jsonParsingCancelledException()
			StringReader(jsonString).use { sr ->
				if (cs.isCancelled) throw jsonParsingCancelledException()
				jackson.readTree(sr)
			}
		}
	}

	private fun jsonParsingCancelledException() = CancellationException("JSON parsing was cancelled.")
}
