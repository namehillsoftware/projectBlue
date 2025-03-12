package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAPlaylistPath

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving a new playlist` {

	private val mut by lazy {
		val fakeConnectionProvider = FakeJRiverConnectionProvider().apply {
			mapResponse(
				{
					FakeConnectionResponseTuple(
						200, (
							"""<Response Status="OK">
<Item>
<Field Name="ID">400003735</Field>
<Field Name="Name">Audible</Field>
<Field Name="Path">Audible</Field>
<Field Name="Type">Group</Field>
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
<Field Name="ID">38981873</Field>
<Field Name="Name">Workout</Field>
<Field Name="Path">Workout</Field>
<Field Name="Type">Playlist</Field>
<Field Name="MediaTypes">Audio</Field>
</Item>
</Response>""").toByteArray()
					)
				},
				"Playlists",
				"List?IncludeMediaTypes=1"
			)

			mapResponse(
				{
					FakeConnectionResponseTuple(
						200, (
							"""<Response Status="OK">
<Item Name="PlaylistID">554772758</Item>
</Response>""").toByteArray()
					)
				},
				"Playlists/Add",
				"Type=Playlist",
				"Path=My Fancy Album",
				"CreateMode=Overwrite",
			)

			mapResponse(
				{
					FakeConnectionResponseTuple(
						200,
						("""<Response Status="OK" />""").toByteArray()
					)
				},
				"Playlist/AddFiles",
				"PlaylistType=ID",
				"&Playlist=554772758",
				"Keys=954,172,366",
			)
		}

		Pair(fakeConnectionProvider, JRiverLibraryAccess(fakeConnectionProvider))
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseStoredPlaylist(
				"My Fancy Album",
				listOf(
					ServiceFile(954),
					ServiceFile(172),
					ServiceFile(366),
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the playlist is created correctly`() {
		assertThat(mut.first.recordedRequests.flatMap { it.asIterable() }).containsExactly(
			"Playlists/Add",
			"Type=Playlist",
			"Path=My Fancy Album",
			"CreateMode=Overwrite",
			"Playlist/AddFiles",
			"PlaylistType=ID",
			"Playlist=554772758",
			"Keys=954,172,366",
		)
	}
}
