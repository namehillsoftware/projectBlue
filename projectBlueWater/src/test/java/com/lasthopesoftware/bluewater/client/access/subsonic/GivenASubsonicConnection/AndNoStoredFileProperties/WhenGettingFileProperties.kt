package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndNoStoredFileProperties

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingFileProperties {
	private val fileProperties by lazy {
		val connection = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "2QbKsOo", "rIuUXpsY"),
			FakeHttpConnectionProvider(
				mockk {
					every { promiseResponse(TestUrl.withSubsonicApi().addPath("getSong").addParams("id=c6c3585f823f4c5093fada347ccd0239")) } returns PassThroughHttpResponse(
						200,
						"Alright",
						"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"song":{"id":"3c00a4d5a48b0790d8e2288faf6fc93c","parent":"9022600b4f57592edd8e8bfc9048fe38","isDir":false,"title":"How Will You Meet Your End","album":"American Hearts","artist":"A.A. Bondy","track":1,"year":2007,"genre":"Americana","coverArt":"mf-3c00a4d5a48b0790d8e2288faf6fc93c_6728cffb","size":25331533,"contentType":"audio/flac","suffix":"flac","duration":242,"bitRate":827,"path":"A.A. Bondy/American Hearts/01 - How Will You Meet Your End.flac","discNumber":1,"created":"2025-02-10T04:25:55.814140973Z","albumId":"9022600b4f57592edd8e8bfc9048fe38","artistId":"563727e73731392260da4a787f534387","type":"music","isVideo":false,"bpm":128,"comment":"Visit http://aabondy.bandcamp.com","sortName":"","mediaType":"song","musicBrainzId":"4fefe475-a23e-44e5-a46a-ce2d4167e108","genres":[{"name":"Americana"}],"replayGain":{"trackGain":-2.34,"trackPeak":0.933,"albumPeak":1},"channelCount":2,"samplingRate":44100}}}""".toByteArray().inputStream()
					).toPromise()
				}
			),
			mockk(),
			JsonEncoderDecoder,
		)

		connection
			.promiseFileProperties(ServiceFile("c6c3585f823f4c5093fada347ccd0239"))
			.toExpiringFuture()
			.get()
	}

    @Test
    fun `then files property key is retrieved`() {
        assertThat(fileProperties!![KnownFileProperties.Key]).isEqualTo("3c00a4d5a48b0790d8e2288faf6fc93c")
    }

	@Test
	fun `then lyric files property is retrieved`() {
		assertThat(fileProperties!![KnownFileProperties.Lyrics]).isEqualTo("""[In the Fade]

[Verse 1: Josh Homme]
Cracks in the ceiling, crooked pictures in the hall
Countin' and breathin', I'm leaving here tomorrow
They don't know, I never do you any good
Laughin' is easy, I would if I could

[Chorus: Mark Lanegan]
Ain't gonna worry
Just live 'til you die, I wanna drown
With nowhere to fall into the arms of someone who
There's nothing to save and I know
You live till you die

[Post-Chorus: Mark Lanegan &amp; Josh Homme]
Live 'til you die
I know
Live 'til you die
I know
Live 'til you die
I know
Live 'til you die
I know

Losing a feeling that I couldn't give away
Counting' and breathing', disappearing' in the fade
They don't know, I never do you any good
Stoppin' and stayin', I would if I could

[Chorus: Mark Lanegan]
Ain't gonna worry
Just live 'til you die, I wanna drown
With nowhere to fall into the arms of someone who
There's nothing to save and I know
You live till you die

[Post-Chorus: Mark Lanegan &amp; Josh Homme]
Live 'til you die
I know
Live 'til you die
I know
Live 'til you die
I know
Live 'til you die
I know

[Feel Good Hit Of The Summer]
Oh
Nicotine, Valium, Vicodin, marijuana, ecstasy, and alcohol (Cocaine!)
Nicotine, Valium, Vicodin, marijuana, ecstasy, and alcohol (Cocaine!)
Nicotine, Valium, Vicodin, marijuana...

Some valid text

Woo hoo

<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>
<span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg>

Some more valid text... la di da...""")
	}
}
