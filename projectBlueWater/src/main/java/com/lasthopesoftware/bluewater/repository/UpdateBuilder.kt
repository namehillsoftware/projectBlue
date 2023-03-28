package com.lasthopesoftware.bluewater.repository

class UpdateBuilder private constructor(tableName: String) {
    private val sqlStringBuilder = StringBuilder("UPDATE $tableName SET ")
    private val setters = ArrayList<String>()
    private var filter = ""

    fun addSetter(columnName: String): UpdateBuilder {
        setters.add(columnName)
        return this
    }

    fun setFilter(filter: String): UpdateBuilder {
        this.filter = filter
        return this
    }

    fun buildQuery(): String {
        for (setter in setters) {
            sqlStringBuilder.append(setter).append(" = @").append(setter)
            if (setter !== setters[setters.size - 1]) sqlStringBuilder.append(", ")
        }
        return sqlStringBuilder.append(' ').append(filter).toString()
    }

    companion object {
        fun fromTable(tableName: String): UpdateBuilder {
            return UpdateBuilder(tableName)
        }
    }
}
