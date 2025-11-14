package com.lasthopesoftware.resources.strings

import com.google.gson.Gson
import com.google.gson.JsonElement

object JsonEncoderDecoder : TranslateJson {
	private val sharedGson by lazy { Gson() }

	override fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T? =
		sharedGson.fromJson(jsonElement, cls)

	override fun <T> parseJson(json: String, cls: Class<T>): T? =
		sharedGson.fromJson(json, cls)

	override fun <T> toJson(obj: T): String =
		sharedGson.toJson(obj) ?: ""
}
