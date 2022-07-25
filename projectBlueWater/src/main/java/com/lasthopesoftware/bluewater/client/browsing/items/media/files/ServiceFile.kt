package com.lasthopesoftware.bluewater.client.browsing.items.media.files

import com.lasthopesoftware.bluewater.shared.IIntKey

class ServiceFile(override var key: Int = 0) : IIntKey<ServiceFile> {

	override operator fun compareTo(other: ServiceFile): Int = key - other.key

    override fun hashCode(): Int = key

    override fun equals(other: Any?): Boolean =
		if (other is ServiceFile) compareTo(other) == 0 else super.equals(other)

    override fun toString(): String = "key: $key"
}
