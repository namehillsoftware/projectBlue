package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndNoStoredFileProperties

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KeyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingFileProperties {
	private val fileProperties by lazy {
		val connection = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "2QbKsOo", "rIuUXpsY"),
			FakeHttpConnectionProvider(
				FakeHttpConnection(
					Pair(
						TestUrl.withSubsonicApi().addPath("getSong").addParams("f=json").addParams("id=c6c3585f823f4c5093fada347ccd0239"),
						{
							PassThroughHttpResponse(
								200,
								"Alright",
								"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"song":{"id":"3c00a4d5a48b0790d8e2288faf6fc93c","parent":"9022600b4f57592edd8e8bfc9048fe38","isDir":false,"title":"How Will You Meet Your End","album":"American Hearts","artist":"A.A. Bondy","track":1,"year":2007,"genre":"Americana","coverArt":"mf-3c00a4d5a48b0790d8e2288faf6fc93c_6728cffb","size":25331533,"contentType":"audio/flac","suffix":"flac","duration":242,"bitRate":827,"path":"A.A. Bondy/American Hearts/01 - How Will You Meet Your End.flac","discNumber":1,"created":"2025-02-10T04:25:55.814140973Z","albumId":"9022600b4f57592edd8e8bfc9048fe38","artistId":"563727e73731392260da4a787f534387","type":"music","isVideo":false,"bpm":128,"comment":"Visit http://aabondy.bandcamp.com","sortName":"","mediaType":"song","musicBrainzId":"4fefe475-a23e-44e5-a46a-ce2d4167e108","genres":[{"name":"Americana"}],"replayGain":{"trackGain":-2.34,"trackPeak":0.933,"albumPeak":1},"channelCount":2,"samplingRate":44100}}}""".toByteArray()
									.inputStream()
							)
						}
					),
					Pair(
						TestUrl.withSubsonicApi().addPath("getLyrics").addParams("f=json").addParams("artist=A.A. Bondy").addParams("title=How Will You Meet Your End"),
						{
							PassThroughHttpResponse(
								200,
								"Yes",
								"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"lyrics":{"artist":"A.A. Bondy","title":"There's a Reason","value":"And they took me around\nThey showed me the Seven Wonders\nThe sights and the sounds\nThere was a man with cinders for eyes\nThere was a girl with a dress made of flies\nAnd there's a reason\nThere's a reason\nAnd it's love that's tearing them down\nAnd it's love that turns them around\nSay it is so\nAnd the ballroom is filled with the joy\nOf making old friends\nAnd jukebox girls trip the light\nThey wiggle and they bend\nBlind Joe, he's feeling no pain\nSweet Georgia, she dreams of the rain\nAnd there's a reason\nThere's a reason\nAnd it's love that's tearing them down\nAnd it's love that will turn them around\nSay it is so\nWhen the moon follows you where you go\nAnd you cannot hide\nAnd when voices of doom ring your ears\nAnd horsemen do ride\nMay tomorrow the land be anew\nMay every bird sing unto you\nThat's the reason\nThat's the reason\nThat the love that's tearing you down\nIs the love that will turn you around\nThat the love that's tearing you down\nIs the love that will turn you around\nSay it is so\n"}}}""".toByteArray().inputStream()
							)
						}
					),
				)
			),
            JsonEncoderDecoder,
			mockk(),
		)

		connection
			.promiseFileProperties(ServiceFile("c6c3585f823f4c5093fada347ccd0239"))
			.toExpiringFuture()
			.get()
	}

    @Test
    fun `then files property key is retrieved`() {
        assertThat(fileProperties?.key).isEqualTo(
			KeyFileProperty("3c00a4d5a48b0790d8e2288faf6fc93c",)
		)
    }

	@Test
	fun `then the track name is correct`() {
		assertThat(fileProperties?.name).isEqualTo(
			ReadOnlyFileProperty(
				NormalizedFileProperties.Name,
				"How Will You Meet Your End"
			)
		)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(fileProperties?.artist).isEqualTo(
			ReadOnlyFileProperty(
				NormalizedFileProperties.Artist,
				"A.A. Bondy"
			)
		)
	}

	@Test
	fun `then the replay gain is correct`() {
		assertThat(fileProperties?.get(NormalizedFileProperties.VolumeLevelReplayGain)).isEqualTo(
			ReadOnlyFileProperty(
				NormalizedFileProperties.VolumeLevelReplayGain,
				"-2.34"
			)
		)
	}

	@Test
	fun `then the peak level is correct`() {
		assertThat(fileProperties?.get(NormalizedFileProperties.PeakLevel)).isEqualTo(
			ReadOnlyFileProperty(
				NormalizedFileProperties.PeakLevel,
				"0.933"
			)
		)
	}

	@Test
	fun `then the rating is editable`() {
		assertThat(fileProperties?.rating).isEqualTo(
			EditableFileProperty(
				NormalizedFileProperties.Rating,
				"0",
				FilePropertyType.Integer,
			)
		)
	}

	@Test
	fun `then lyric files property is retrieved`() {
		assertThat(fileProperties?.get(NormalizedFileProperties.Lyrics)?.value).isEqualToIgnoringWhitespace("""And they took me around
  They showed me the Seven Wonders
  The sights and the sounds
  There was a man with cinders for eyes
  There was a girl with a dress made of flies
  And there's a reason
  There's a reason
  And it's love that's tearing them down
  And it's love that turns them around
  Say it is so
  And the ballroom is filled with the joy
  Of making old friends
  And jukebox girls trip the light
  They wiggle and they bend
  Blind Joe, he's feeling no pain
  Sweet Georgia, she dreams of the rain
  And there's a reason
  There's a reason
  And it's love that's tearing them down
  And it's love that will turn them around
  Say it is so
  When the moon follows you where you go
  And you cannot hide
  And when voices of doom ring your ears
  And horsemen do ride
  May tomorrow the land be anew
  May every bird sing unto you
  That's the reason
  That's the reason
  That the love that's tearing you down
  Is the love that will turn you around
  That the love that's tearing you down
  Is the love that will turn you around
  Say it is so""")
	}
}
