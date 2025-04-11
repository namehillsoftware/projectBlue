package com.lasthopesoftware.resources.strings

import com.google.gson.JsonElement

interface TranslateJson {
	fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T?
	fun <T> parseJson(json: String, cls: Class<T>): T?
	fun <T> toJson(obj: T): String
}

inline fun <reified T> TranslateJson.parseJson(jsonElement: JsonElement): T? = parseJson(jsonElement, T::class.java)
inline fun <reified T> TranslateJson.parseJson(json: String): T? = parseJson(json, T::class.java)
