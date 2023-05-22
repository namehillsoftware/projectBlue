package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

object QueueProviders {
    private val lazyProviders by lazy { listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()) }

    fun providers(): Iterable<ProvidePositionedFileQueue> = lazyProviders
}
