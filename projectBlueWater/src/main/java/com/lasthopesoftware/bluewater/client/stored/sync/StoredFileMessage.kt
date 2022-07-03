package com.lasthopesoftware.bluewater.client.stored.sync

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface StoredFileMessage : ApplicationMessage {
	val storedFileId: Int

	class FileQueued(override val storedFileId: Int) : StoredFileMessage
	class FileDownloading(override val storedFileId: Int) : StoredFileMessage
	class FileDownloaded(override val storedFileId: Int) : StoredFileMessage
	class FileWriteError(override val storedFileId: Int) : StoredFileMessage
	class FileReadError(override val storedFileId: Int) : StoredFileMessage
}
