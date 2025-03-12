package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndNoStoredFileProperties

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingFileProperties {
	private val fileProperties by lazy {
		val fakeFileConnectionProvider = FakeJRiverConnectionProvider()
		fakeFileConnectionProvider.setupFile(
			ServiceFile(15),
			mapOf(
				Pair(KnownFileProperties.Key, "45"),
				Pair(KnownFileProperties.Lyrics, """[In the Fade]

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

		val filePropertiesProvider = JRiverLibraryAccess(fakeFileConnectionProvider)
		filePropertiesProvider
			.promiseFileProperties(ServiceFile(15))
			.toExpiringFuture()
			.get()
	}

    @Test
    fun `then files property key is retrieved`() {
        assertThat(fileProperties!![KnownFileProperties.Key]).isEqualTo("45")
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
