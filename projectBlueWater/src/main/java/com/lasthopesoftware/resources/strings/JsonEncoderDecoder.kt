package com.lasthopesoftware.resources.strings

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.memory.MemoryBanks
import com.google.gson.memory.StringBuilderBank
import com.google.gson.reflect.TypeToken
import java.io.Reader

object JsonEncoderDecoder : TranslateJson {
	private val gson by lazy {
		MemoryBanks.setStringBuilderBank(StringBuilderBank())
		Gson()
	}

	override fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T? =
		gson.fromJson(jsonElement, cls)

	override fun <T> parseJson(reader: Reader, cls: Class<T>): T? =
		gson.fromJson(reader, cls)

	override fun <T> parseJson(json: String, cls: Class<T>): T? =
		gson.fromJson(json, cls)

	override fun <T> parseJson(json: String, typeToken: TypeToken<T>): T? =
		gson.fromJson(json, typeToken)

	override fun <T> toJson(obj: T): String =
		gson.toJson(obj) ?: ""
}
