package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import io.reactivex.Observable

interface ProcessStoredFileJobs {
    fun observeStoredFileDownload(jobs: Iterable<StoredFileJob>): Observable<StoredFileJobStatus>
}
