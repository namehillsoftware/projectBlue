package com.lasthopesoftware.resources.strings

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser

object JsonEncoderDecoder : TranslateJson, TranslateGson {
	private val sharedGson by lazy { Gson() }

	override fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T? = sharedGson.fromJson(jsonElement, cls)

	override fun <T> parseJson(json: String, cls: Class<T>): T? = sharedGson.fromJson(json, cls)

	override fun parseJsonElement(jsonString: String): JsonElement = JsonParser.parseString(jsonString)

	override fun toJsonElement(obj: Any): JsonElement = sharedGson.toJsonTree(obj)

	override fun serializeJson(jsonElement: JsonElement): String = sharedGson.toJson(jsonElement)

	override fun serializeJson(obj: Any): String = sharedGson.toJson(obj) ?: ""
}
