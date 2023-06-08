package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.Entity
import java.util.Objects

@Keep
class StoredFile : Entity {
    var id = 0
        private set
    var libraryId = 0
        private set
    var storedMediaId = 0
        private set
    var serviceId = 0
        private set
    var isDownloadComplete = false
        private set
    var path: String? = null
        private set
    var isOwner = false
        private set

    constructor()
    constructor(
        libraryId: LibraryId,
        storedMediaId: Int,
        serviceFile: ServiceFile,
        path: String?,
        isOwner: Boolean
    ) {
        this.libraryId = libraryId.id
        this.storedMediaId = storedMediaId
        serviceId = serviceFile.key
        this.path = path
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

    fun setStoredMediaId(storedMediaId: Int): StoredFile {
        this.storedMediaId = storedMediaId
        return this
    }

    fun setServiceId(serviceId: Int): StoredFile {
        this.serviceId = serviceId
        return this
    }

    fun setIsDownloadComplete(isDownloadComplete: Boolean): StoredFile {
        this.isDownloadComplete = isDownloadComplete
        return this
    }

    fun setPath(path: String?): StoredFile {
        this.path = path
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
                ", storedMediaId=" + storedMediaId +
                ", serviceId=" + serviceId +
                ", isDownloadComplete=" + isDownloadComplete +
                ", path='" + path + '\'' +
                ", isOwner=" + isOwner +
                '}'
    }
}
