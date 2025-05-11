package com.lasthopesoftware.resources.strings

import com.google.gson.Gson
import com.google.gson.JsonElement

object JsonEncoderDecoder : TranslateJson {
	private val threadLocalGson = ThreadLocal.withInitial { Gson() }

	override fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T? =
		threadLocalGson.get()?.fromJson(jsonElement, cls)

	override fun <T> parseJson(json: String, cls: Class<T>): T? =
		threadLocalGson.get()?.fromJson(json, cls)

	override fun <T> toJson(obj: T): String =
		threadLocalGson.get()?.toJson(obj) ?: ""
}
