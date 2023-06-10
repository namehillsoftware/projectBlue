package com.lasthopesoftware.bluewater.client.browsing.items.playlists.GivenALibraryAndPath

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistsStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 24

class `When updating a playlist` {

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
<Item Name="PlaylistID">38981873</Item>
</Response>""").toByteArray()
                    )
				},
				"Playlists",
				"Add?Type=Playlist&Path=bold\\voyage&CreateMode=Overwrite"
			)

			mapResponse(
				{
                    FakeConnectionResponseTuple(
                        200,
                        ("""<Response Status="OK" />""").toByteArray()
                    )
				},
				"Playlist",
				"AddFiles?PlaylistType=ID&Playlist=38981873&Keys=885,481,139,935"
			)
		}

		Pair(
			fakeConnectionProvider,
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
        )
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseStoredPlaylist(
                LibraryId(libraryId),
				"bold\\voyage",
				listOf(
                    ServiceFile(885),
                    ServiceFile(481),
                    ServiceFile(139),
                    ServiceFile(935),
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the playlist is created correctly`() {
		assertThat(mut.first.recordedRequests.flatMap { it.asIterable() }).containsExactly(
			"Playlists",
			"Add?Type=Playlist&Path=bold\\voyage&CreateMode=Overwrite",
			"Playlist",
			"AddFiles?PlaylistType=ID&Playlist=38981873&Keys=885,481,139,935",
		)
	}
}
