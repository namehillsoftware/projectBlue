package com.lasthopesoftware.resources.strings

import com.namehillsoftware.handoff.promises.Promise
import tools.jackson.databind.JsonNode

interface TranslateJson {
	fun <T> parseJson(jsonElement: JsonNode, cls: Class<T>): T?
	fun <T> parseJson(json: String, cls: Class<T>): T?
	fun <T> toJson(obj: T): String

	fun Promise<String>.promiseJson(): Promise<JsonNode?>
}

inline fun <reified T> TranslateJson.parseJson(jsonElement: JsonNode): T? = parseJson(jsonElement, T::class.java)
inline fun <reified T> TranslateJson.parseJson(json: String): T? = parseJson(json, T::class.java)
