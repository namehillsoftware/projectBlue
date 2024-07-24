package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenAFileProperty.AndItHasXmlInTheText

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.getFormattedValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Getting the Formatted Value` {
	private val fileProperty by lazy {
		FileProperty(
			"wosmPjL6",
			"""
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

Some more valid text... la di da...""")
	}

	private var formattedValue = ""

	@BeforeAll
	fun act() {
		formattedValue = fileProperty.getFormattedValue()
	}

	@Test
	fun `then the formatted value is correct`() {
		assertThat(formattedValue).isEqualTo("""
Some valid text

Woo hoo



Some more valid text... la di da...""")
	}
}
