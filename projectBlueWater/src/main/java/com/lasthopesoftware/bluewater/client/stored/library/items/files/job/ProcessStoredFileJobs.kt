package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import io.reactivex.rxjava3.core.Observable

interface ProcessStoredFileJobs {
	fun observeStoredFileDownload(jobs: Observable<StoredFileJob>): Observable<StoredFileJobStatus>
}
