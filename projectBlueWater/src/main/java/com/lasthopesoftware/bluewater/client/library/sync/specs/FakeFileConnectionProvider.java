package com.lasthopesoftware.bluewater.client.library.sync.specs;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;


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
}
