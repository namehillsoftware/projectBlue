package com.lasthopesoftware.bluewater.repository

import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

inline fun <reified T> Artful.fetchFirst(): T = fetchFirst(T::class.java)

inline fun <reified T> Artful.fetch(): List<T> = fetch(T::class.java)

val propertyCache = ConcurrentHashMap<KClass<*>, Collection<KProperty1<*, *>>>()
val insertStatementCache = ConcurrentHashMap<String, String>()
val updateStatementCache = ConcurrentHashMap<String, String>()

inline fun <reified T : Entity> RepositoryAccessHelper.insert(value: T): Long {
	val kClass = T::class
	val tableName = kClass.simpleName ?: return 0

	return insert(tableName, value)
}

inline fun <reified T : Entity> RepositoryAccessHelper.insert(tableName: String, value: T): Long {
	val properties = T::class.declaredMemberProperties

	val insertQuery = insertStatementCache.getOrPut(tableName) {
		val insertBuilder = InsertBuilder.fromTable(tableName)
		for (property in properties) {
			insertBuilder.addColumn(property.name)
		}

		insertBuilder.build()
	}

	beginTransaction().use { transaction ->
		val artful = mapSql(insertQuery)
		for (property in properties) {
			artful.addParameter(property.name, property.get(value))
		}

		return artful.execute().also {
			transaction.setTransactionSuccessful()
		}
	}
}

inline fun <reified T : Entity> RepositoryAccessHelper.update(tableName: String, value: T): Long {
	val properties = T::class.declaredMemberProperties

	val updateQuery = updateStatementCache.getOrPut(tableName) {
		val updateBuilder = UpdateBuilder.fromTable(tableName)
		for (property in properties) {
			updateBuilder.addSetter(property.name)
		}

		updateBuilder.setFilter("WHERE id = @id")

		updateBuilder.buildQuery()
	}

	beginTransaction().use { transaction ->
		val artful = mapSql(updateQuery)
		for (property in properties) {
			artful.addParameter(property.name, property.getter.call(value))
		}

		return artful.execute().also {
			transaction.setTransactionSuccessful()
		}
	}
}

inline fun <reified T> Promise<Artful>.promiseFirst(): Promise<T> =
	then { it.fetchFirst(T::class.java) }

inline fun <reified T> Artful.promiseFirst(): Promise<T> =
	DatabasePromise { fetchFirst(T::class.java) }

fun Promise<Artful>.promiseExecution(): Promise<Long> =
	then { it.execute() }

fun Artful.promiseExecution(): Promise<Long> =
	DatabasePromise { execute() }
