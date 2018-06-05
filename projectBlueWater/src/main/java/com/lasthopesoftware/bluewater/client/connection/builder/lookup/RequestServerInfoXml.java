package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import xmlwise.XmlElement;

public interface RequestServerInfoXml {
	Promise<XmlElement> promiseServerInfoXml(Library library);
}
