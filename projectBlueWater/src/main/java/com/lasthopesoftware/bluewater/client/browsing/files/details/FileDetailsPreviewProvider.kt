package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.DateTime
import org.joda.time.Duration

class FileDetailsPreviewProvider : PreviewParameterProvider<FileDetailsViewModel> {
	override val values: Sequence<FileDetailsViewModel>
		get() = sequenceOf(
			FileDetailsViewModel(
				object : ProvideScopedFileProperties {
					private val duration = Duration.standardMinutes(5).millis
					private val lastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

					override fun promiseFileProperties(serviceFile: ServiceFile) = Promise(
						mapOf(
							Pair("Key", "23"),
							Pair("Media Type", "Audio"),
							Pair(KnownFileProperties.LAST_PLAYED, lastPlayed.toString()),
							Pair("Rating", "4"),
							Pair("File Size", "2345088"),
							Pair(KnownFileProperties.DURATION, duration.toString()),
						)
					)
				},
				object : ProvideDefaultImage {
					override fun promiseFileBitmap(): Promise<Bitmap> =
						Promise(BitmapFactory.decodeByteArray(ByteArray(0), 0, 0))
				},
				object : ProvideImages {
					override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
						Promise(BitmapFactory.decodeByteArray(ByteArray(0), 0, 0))
				}
			)
		)
}
