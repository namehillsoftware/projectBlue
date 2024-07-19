package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenAFileProperty.AndItHasHtmlEncodedText

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.getFormattedValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Getting the Formatted Value` {
	private val fileProperty by lazy {
		FileProperty(
			"krkZz00hIDg",
			"""[In the Fade]

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
Nicotine, Valium, Vicodin, marijuana...""")
	}

	private var formattedValue = ""

	@BeforeAll
	fun act() {
		formattedValue = fileProperty.getFormattedValue()
	}

	@Test
	fun `then the formatted value is correct`() {
		assertThat(formattedValue).isEqualTo("""[In the Fade]

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
Nicotine, Valium, Vicodin, marijuana...""")
	}
}
