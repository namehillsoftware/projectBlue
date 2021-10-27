package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.namehillsoftware.handoff.promises.Promise

interface ReceiveStoredFileEvent {
	fun receive(storedFileId: Int): Promise<Unit>
	fun acceptedEvents(): Collection<String>
}
