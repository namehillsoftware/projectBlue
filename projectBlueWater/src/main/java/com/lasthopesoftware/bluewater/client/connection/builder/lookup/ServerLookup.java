package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import xmlwise.XmlElement;

import java.util.Arrays;

public class ServerLookup implements LookupServers {

	private static final String ipElement = "ip";
	private static final String localIpListElement = "localiplist";
	private static final String portElement = "port";
	private static final String httpsPortElement = "https_port";
	private static final String certificateFingerprintElement = "certificate_fingerprint";
	private static final String statusAttribute = "Status";
	private static final String errorStatusValue = "Error";
	private static final String msgElement = "msg";

	private final RequestServerInfoXml serverInfoXmlRequest;

	public ServerLookup(RequestServerInfoXml serverInfoXmlRequest) {
		this.serverInfoXmlRequest = serverInfoXmlRequest;
	}

	@Override
	public Promise<ServerInfo> promiseServerInformation(Library library) {
		return serverInfoXmlRequest.promiseServerInfoXml(library)
			.then(xml -> {
				if (xml == null) return null;

				if (xml.containsAttribute(statusAttribute) && errorStatusValue.equals(xml.getAttribute(statusAttribute))) {
					if (xml.contains(msgElement))
						throw new ServerDiscoveryException(library, xml.getUnique(msgElement).getValue());
					throw new ServerDiscoveryException(library);
				}

				final ServerInfo serverInfo = new ServerInfo();

				final XmlElement remoteIp = xml.getUnique(ipElement);
				serverInfo.setRemoteIp(remoteIp.getValue());

				final XmlElement localIps = xml.getUnique(localIpListElement);
				serverInfo.setLocalIps(Arrays.asList(localIps.getValue().split(",")));

				final XmlElement portXml = xml.getUnique(portElement);
				serverInfo.setHttpPort(Integer.parseInt(portXml.getValue()));

				if (xml.contains(httpsPortElement))
					serverInfo.setHttpsPort(Integer.parseInt(xml.getUnique(httpsPortElement).getValue()));

				if (xml.contains(certificateFingerprintElement))
					serverInfo.setCertificateFingerprint(xml.getUnique(certificateFingerprintElement).getValue());

				return serverInfo;
			});
	}
}
