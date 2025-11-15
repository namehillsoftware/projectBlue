package com.lasthopesoftware.resources.strings

import com.google.gson.JsonElement

interface TranslateGson {
	fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T?
	fun parseJsonElement(jsonString: String): JsonElement
	fun toJsonElement(obj: Any): JsonElement
	fun serializeJson(jsonElement: JsonElement): String
}

inline fun <reified T> TranslateGson.parseJson(jsonElement: JsonElement): T? = parseJson(jsonElement, T::class.java)
