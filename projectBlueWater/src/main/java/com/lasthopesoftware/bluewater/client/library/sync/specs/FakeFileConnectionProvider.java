package com.lasthopesoftware.bluewater.client.library.sync.specs;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

import java.util.Map;


public class FakeFileConnectionProvider extends FakeConnectionProvider {
	public FakeFileConnectionProvider() {
		super();

		mapResponse(
			(params) -> new byte[0],
			"File/GetFile",
			"File=.*",
			"Quality=medium",
			"Conversion=Android",
			"Playback=0");
	}

	public void setupFile(ServiceFile serviceFile, Map<String, String> fileProperties) {
		mapResponse(params -> {
				final StringBuilder returnXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
					"<MPL Version=\"2.0\" Title=\"MCWS - Files - 10936\" PathSeparator=\"\\\">\n" +
					"<Item>\n" +
					"<Field Name=\"Key\">" + serviceFile.getKey() + "</Field>\n" +
					"<Field Name=\"Media Type\">Audio</Field>\n");

				for (Map.Entry<String, String> property : fileProperties.entrySet())
					returnXml.append("<Field Name=\"").append(property.getKey()).append("\">").append(property.getValue()).append("</Field>\n");

				returnXml.append("</Item>\n" + "</MPL>\n");

				return returnXml.toString().getBytes();
			},
			"File/GetInfo",
			"File=" + serviceFile.getKey());
	}
}
