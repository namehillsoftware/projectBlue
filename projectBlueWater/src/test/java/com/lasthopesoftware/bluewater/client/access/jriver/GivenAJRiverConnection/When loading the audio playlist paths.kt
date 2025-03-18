package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the audio playlist paths` {
	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Playlists/List", "IncludeMediaTypes=1")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""<Response Status="OK">
<Item>
<Field Name="ID">400003735</Field>
<Field Name="Name">Audible</Field>
<Field Name="Path">Audible</Field>
<Field Name="Type">Group</Field>
</Item>
<Item>
<Field Name="ID">38981873</Field>
<Field Name="Name">Workout</Field>
<Field Name="Path">Workout</Field>
<Field Name="Type">Playlist</Field>
<Field Name="MediaTypes">Audio</Field>
</Item>
<Item>
<Field Name="ID">787975935</Field>
<Field Name="Name">4A</Field>
<Field Name="Path">Nested\C8ot8h</Field>
<Field Name="Type">Playlist</Field>
<Field Name="MediaTypes">Video</Field>
</Item>
<Item>
<Field Name="ID">326355669</Field>
<Field Name="Name">Audible Titles</Field>
<Field Name="Path">Audible\Audible Titles</Field>
<Field Name="Type">Smartlist</Field>
</Item>
<Item>
<Field Name="ID">909745080</Field>
<Field Name="Name">Car Radio</Field>
<Field Name="Path">Car Radio</Field>
<Field Name="Type">Group</Field>
</Item>
<Item>
<Field Name="ID">787975935</Field>
<Field Name="Name">4A</Field>
<Field Name="Path">Nested\4A</Field>
<Field Name="Type">Playlist</Field>
<Field Name="MediaTypes">Audio</Field>
</Item>
</Response>""".encodeToByteArray().inputStream()
				)
			}
		}

		MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}

	private var audioPlaylist: List<String>? = null

	@BeforeAll
	fun act() {
		audioPlaylist = mut.promiseAudioPlaylistPaths().toExpiringFuture().get()
	}

	@Test
	fun `then the audio playlist is correct`() {
		assertThat(audioPlaylist).containsExactly(
			"Workout",
			"Nested\\4A"
		)
	}
}
