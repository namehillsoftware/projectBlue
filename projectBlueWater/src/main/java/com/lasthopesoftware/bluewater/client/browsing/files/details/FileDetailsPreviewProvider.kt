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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import org.joda.time.DateTime
import org.joda.time.Duration
import java.net.URL

class FileDetailsPreviewProvider : PreviewParameterProvider<FileDetailsViewModel> {
	override val values: Sequence<FileDetailsViewModel>
		get() = sequenceOf(
			FileDetailsViewModel(
				object : ProvideSelectedConnection {
					override fun promiseSessionConnection(): Promise<IConnectionProvider?> =
						Promise(object : IConnectionProvider {
							override fun promiseResponse(vararg params: String): Promise<Response> {
								val duration = Duration.standardMinutes(5).millis
								val lastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

								val bytes = """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">23</Field>
<Field Name="Media Type">Audio</Field>
<Field Name="${KnownFileProperties.LAST_PLAYED}">$lastPlayed</Field>
<Field Name="Rating">4</Field>
<Field Name="File Size">2345088</Field>
<Field Name="${KnownFileProperties.DURATION}">$duration</Field>
</Item>
</MPL>
""".toByteArray()

								val buffer = Buffer()
								buffer.write(bytes, 0, bytes.size)

								return Response.Builder()
									.body(RealResponseBody(null, bytes.size.toLong(), buffer))
									.code(200)
									.build()
									.toPromise()
							}

							override val urlProvider: IUrlProvider
								get() = object : IUrlProvider {
									override fun getUrl(vararg params: String): String? = null

									override val baseUrl: URL?
										get() = null
									override val authCode: String?
										get() = null
									override val certificateFingerprint: ByteArray?
										get() = ByteArray(0)
								}
						})
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
