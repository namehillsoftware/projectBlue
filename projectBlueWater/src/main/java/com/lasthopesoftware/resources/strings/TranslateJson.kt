package com.lasthopesoftware.resources.strings

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.ktor.util.collections.ConcurrentMap
import java.io.Reader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface TranslateJson {
	fun <T> parseJson(jsonElement: JsonElement, cls: Class<T>): T?
	fun <T> parseJson(json: String, cls: Class<T>): T?
	fun <T> parseJson(json: String, typeToken: TypeToken<T>): T?
	fun <T> toJson(obj: T): String
	fun <T> parseJson(reader: Reader, cls: Class<T>): T?
}

val kTypeTokenMap = ConcurrentMap<KType, TypeToken<*>>()

@Suppress("UNCHECKED_CAST")
inline fun <reified T> TranslateJson.parseJson(json: String): T? {
	val kType = typeOf<T>()
	return parseJson(json, kTypeTokenMap.computeIfAbsent(kType) { object : TypeToken<T>() {} } as TypeToken<T>)
}
