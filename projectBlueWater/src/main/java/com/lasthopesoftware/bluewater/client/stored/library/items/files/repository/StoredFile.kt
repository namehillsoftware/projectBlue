package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import java.net.URI
import java.util.Objects

@Keep
class StoredFile() : IdentifiableEntity {
	override var id = 0
		private set
	var libraryId = 0
		private set
	var serviceId = ""
		private set
	var isDownloadComplete = false
		private set
	var uri: String? = null
		private set
	var isOwner = false
		private set

	constructor(
		libraryId: LibraryId,
		serviceFile: ServiceFile,
		uri: URI?,
		isOwner: Boolean
	) : this() {
		this.libraryId = libraryId.id
		serviceId = serviceFile.key
		this.uri = uri?.toString()
		this.isOwner = isOwner
	}

	fun setId(id: Int): StoredFile {
		this.id = id
		return this
	}

	fun setLibraryId(libraryId: Int): StoredFile {
		this.libraryId = libraryId
		return this
	}

	fun setServiceId(serviceId: String): StoredFile {
		this.serviceId = serviceId
		return this
	}

	fun setIsDownloadComplete(isDownloadComplete: Boolean): StoredFile {
		this.isDownloadComplete = isDownloadComplete
		return this
	}

	fun setUri(uri: String?): StoredFile {
		this.uri = uri
		return this
	}

	fun setIsOwner(isOwner: Boolean): StoredFile {
		this.isOwner = isOwner
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || javaClass != other.javaClass) return false
		val that = other as StoredFile
		return libraryId == that.libraryId &&
			serviceId == that.serviceId
	}

	override fun hashCode(): Int {
		return Objects.hash(libraryId, serviceId)
	}

	override fun toString(): String {
		return "StoredFile{" +
			"id=" + id +
			", libraryId=" + libraryId +
			", serviceId=" + serviceId +
			", isDownloadComplete=" + isDownloadComplete +
			", uri='" + uri + '\'' +
			", isOwner=" + isOwner +
			'}'
	}
}
