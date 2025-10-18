package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.bluewater.IdentifiableEntity
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import java.io.IOException

inline fun <reified T> SqLiteCommand.fetchFirstOrNull(): T? = fetchFirst(T::class.java)

inline fun <reified T> SqLiteCommand.fetch(): List<T> = fetch(T::class.java)

private const val idField = "id"

fun <T : Entity> RepositoryAccessHelper.insert(tableName: String, value: T): T {
	return beginTransaction().use { transaction ->
		val result = SqLiteAssistants.insertValue(writableDatabase, tableName, value)

		if (result == 0L) {
			throw IOException("Insert into $tableName returned 0 rows.")
		}

		transaction.setTransactionSuccessful()

		mapSql("SELECT * FROM $tableName WHERE rowId = (SELECT MAX(rowId) FROM $tableName)").fetchFirst(value::class.java)
	}
}

fun <T : IdentifiableEntity> RepositoryAccessHelper.update(tableName: String, value: T): T {
	beginTransaction().use { transaction ->
		val result = SqLiteAssistants.updateValue(writableDatabase, tableName, value)

		if (result == 0L) {
			throw IOException("Updating $tableName for id ${value.id} returned 0 rows.")
		}

		transaction.setTransactionSuccessful()

		return mapSql("SELECT * FROM $tableName where $idField = @$idField")
			.addParameter(idField, value.id)
			.fetchFirst(value::class.java)
	}
}
