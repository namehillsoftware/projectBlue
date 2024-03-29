package com.lasthopesoftware.bluewater.client.browsing.items.playlists.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistsStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 180

class `When loading the audio playlist paths` {

	private val mut by lazy {
		val fakeConnectionProvider = FakeConnectionProvider().apply {
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
</Response>""").toByteArray()
					)
				},
				"Playlists/List",
				"IncludeMediaTypes=1"
			)
		}

		PlaylistsStorage(
			FakeLibraryConnectionProvider(
				mapOf(
					Pair(
						LibraryId(libraryId),
						fakeConnectionProvider
					)
				)
			)
		)
	}

	private var audioPlaylist: List<String>? = null

	@BeforeAll
	fun act() {
		audioPlaylist = mut.promiseAudioPlaylistPaths(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the audio playlist is correct`() {
		assertThat(audioPlaylist).containsExactly(
			"Workout",
			"Nested\\4A"
		)
	}
}
