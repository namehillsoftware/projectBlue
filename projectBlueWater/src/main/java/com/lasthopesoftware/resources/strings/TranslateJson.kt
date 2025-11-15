package com.lasthopesoftware.resources.strings

interface TranslateJson {
	fun <T> parseJson(json: String, cls: Class<T>): T?
	fun serializeJson(obj: Any): String
}

inline fun <reified T> TranslateJson.parseJson(json: String): T? = parseJson(json, T::class.java)
