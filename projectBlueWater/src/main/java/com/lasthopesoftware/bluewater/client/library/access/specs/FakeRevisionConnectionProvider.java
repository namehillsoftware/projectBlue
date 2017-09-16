package com.lasthopesoftware.bluewater.client.library.access.specs;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;


public class FakeRevisionConnectionProvider extends FakeConnectionProvider {

	private int syncRevision;

	public FakeRevisionConnectionProvider() {
		mapResponse(
			(params) ->
				("<Response Status=\"OK\">" +
					"<Item Name=\"Master\">1192</Item>" +
					"<Item Name=\"Sync\">" + syncRevision + "</Item>" +
					"<Item Name=\"LibraryStartup\">1501430846</Item>" +
				"</Response>").getBytes(),
			"Library/GetRevision");
	}

	public void setSyncRevision(int syncRevision) {
		this.syncRevision = syncRevision;
	}
}
