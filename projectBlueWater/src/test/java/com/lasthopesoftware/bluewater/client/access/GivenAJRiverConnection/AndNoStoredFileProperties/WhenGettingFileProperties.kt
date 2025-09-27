package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndNoStoredFileProperties

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.pow

class WhenGettingFileProperties {
	private val fileProperties by lazy {
		val httpConnection = FakeHttpConnection().apply {
			setupFile(
				TestMcwsUrl.addPath("File/GetInfo").addParams("File=15"),
				ServiceFile("15"),
				mapOf(
					Pair(NormalizedFileProperties.Key, "45"),
					Pair("Peak Level (Sample)", "308.99 dB; -0.3 Left; -0.5 Right"),
					Pair(NormalizedFileProperties.Lyrics, """[In the Fade]

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

[Post-Chorus: Mark Lanegan &amp;amp; Josh Homme]
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

[Post-Chorus: Mark Lanegan &amp;amp; Josh Homme]
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

&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;
&lt;span jsname="Bil8Ae" class="xTFaxe z1asCe SaPW2b" style="height:18px;line-height:18px;width:18px"&gt;&lt;svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 24 24"&gt;&lt;path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"&gt;&lt;/path&gt;&lt;/svg&gt;

Some more valid text... la di da..."""),
				)
			)
		}

		val connection = LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)

		connection
			.promiseFileProperties(ServiceFile("15"))
			.toExpiringFuture()
			.get()
	}

    @Test
    fun `then files property key is retrieved`() {
        assertThat(fileProperties!![NormalizedFileProperties.Key]).isEqualTo("45")
    }

	@Test
	fun `then the peak level is correct`() {
		assertThat(fileProperties!![NormalizedFileProperties.PeakLevel]).isEqualTo(10.0.pow(308.99 / 20).toString())
	}

	@Test
	fun `then the peak level sample property is correct`() {
		assertThat(fileProperties!!["Peak Level (Sample)"]).isEqualTo("308.99 dB; -0.3 Left; -0.5 Right")
	}

	@Test
	fun `then lyric files property is retrieved`() {
		assertThat(fileProperties!![NormalizedFileProperties.Lyrics]).isEqualTo("""[In the Fade]

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
