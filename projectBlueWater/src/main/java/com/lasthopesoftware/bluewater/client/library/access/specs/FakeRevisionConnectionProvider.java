package com.lasthopesoftware.bluewater.client.library.access.specs;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.messenger.promises.Promise;


public class FakeRevisionConnectionProvider extends FakeConnectionProvider {

	private int syncRevision;

	public FakeRevisionConnectionProvider() {
		mapResponse(
			(params) ->
				"<Response Status=\"OK\">" +
					"<Item Name=\"Master\">1192</Item>" +
					"<Item Name=\"Sync\">" + syncRevision + "</Item>" +
					"<Item Name=\"LibraryStartup\">1501430846</Item>" +
				"</Response>",
			"Library/GetRevision");
	}

	@Override
	public Promise<SemanticVersion> promiseConnectionProgramVersion() {
		return new Promise<>(new SemanticVersion(1, 0, 0));
	}

	public void setSyncRevision(int syncRevision) {
		this.syncRevision = syncRevision;
	}
}
