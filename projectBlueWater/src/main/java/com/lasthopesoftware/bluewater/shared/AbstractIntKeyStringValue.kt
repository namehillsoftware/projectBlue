package com.lasthopesoftware.bluewater.shared

import java.util.*

abstract class AbstractIntKeyStringValue protected constructor(
	override var key: Int = 0,
	override var value: String? = null)
: IIntKeyStringValue {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AbstractIntKeyStringValue
        return key == that.key && value == that.value
    }

    override fun hashCode(): Int = Objects.hash(key, value)
}
