package com.lasthopesoftware.bluewater.client.playback.file.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.duration
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.extensions.cancelBackThen
import org.joda.time.Duration

class FilePropertiesDurationReader(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val libraryId: LibraryId,
	private val serviceFile: ServiceFile,
) : ReadFileDuration {
	override val duration by RetryOnRejectionLazyPromise {
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.cancelBackThen { fp, _ ->
				fp.duration ?: Duration.ZERO
			}
	}
}
