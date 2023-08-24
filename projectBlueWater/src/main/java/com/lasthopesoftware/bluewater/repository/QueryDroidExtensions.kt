package com.lasthopesoftware.bluewater.repository

import com.namehillsoftware.artful.Artful
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.declaredMemberProperties

inline fun <reified T> Artful.fetchFirst(): T = fetchFirst(T::class.java)

inline fun <reified T> Artful.fetch(): List<T> = fetch(T::class.java)

private const val idField = "id"

private val insertStatementCache = ConcurrentHashMap<String, String>()
private val updateStatementCache = ConcurrentHashMap<String, String>()

fun <T : Entity> RepositoryAccessHelper.insert(tableName: String, value: T): T {
	val properties = value::class.declaredMemberProperties

	val insertQuery = insertStatementCache.getOrPut(tableName) {
		val insertBuilder = InsertBuilder.fromTable(tableName)
		for (property in properties) {
			if (property.name != idField)
				insertBuilder.addColumn(property.name)
		}

		insertBuilder.build()
	}

	return beginTransaction().use { transaction ->
		val artful = mapSql(insertQuery)
		for (property in properties) {
			if (property.name != idField)
				artful.addParameter(property.name, property.getter.call(value))
		}

		if (artful.execute() == 0L) {
			throw IOException("Insert into $tableName returned 0 rows.")
		}

		transaction.setTransactionSuccessful()

		mapSql("SELECT * FROM $tableName WHERE rowId = (SELECT MAX(rowId) FROM $tableName)").fetchFirst(value::class.java)
	}
}

fun <T : Entity> RepositoryAccessHelper.update(tableName: String, value: T): T {
	val properties = value::class.declaredMemberProperties

	val updateQuery = updateStatementCache.getOrPut(tableName) {
		val updateBuilder = UpdateBuilder.fromTable(tableName)
		for (property in properties) {
			if (property.name != "id")
				updateBuilder.addSetter(property.name)
		}

		updateBuilder.setFilter("WHERE id = @id")

		updateBuilder.buildQuery()
	}

	beginTransaction().use { transaction ->
		val artful = mapSql(updateQuery)

		var id: Any? = null
		for (property in properties) {
			val propertyValue = property.getter.call(value)
			if (property.name == idField)
				id = propertyValue

			artful.addParameter(property.name, propertyValue)
		}

		if (id == null) {
			throw IllegalArgumentException("Table must have id field.")
		}

		if (artful.execute() == 0L) {
			throw IOException("Updating $tableName for id $id returned 0 rows.")
		}

		transaction.setTransactionSuccessful()

		return mapSql("SELECT * FROM $tableName where id = @id")
			.addParameter(idField, id)
			.fetchFirst(value::class.java)
	}
}
