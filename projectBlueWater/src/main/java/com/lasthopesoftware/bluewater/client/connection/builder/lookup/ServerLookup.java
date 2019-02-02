package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import xmlwise.XmlElement;

import java.util.Arrays;

public class ServerLookup implements LookupServers {

	private static final String ipKey = "ip";
	private static final String localIpListKey = "localiplist";
	private static final String portKey = "port";
	private static final String httpsPortKey = "https_port";
	private static final String certificateFingerprintKey = "certificate_fingerprint";

	private final RequestServerInfoXml serverInfoXmlRequest;

	public ServerLookup(RequestServerInfoXml serverInfoXmlRequest) {
		this.serverInfoXmlRequest = serverInfoXmlRequest;
	}

	@Override
	public Promise<ServerInfo> promiseServerInformation(Library library) {
		return serverInfoXmlRequest.promiseServerInfoXml(library)
			.then(xml -> {
				if (xml == null) return null;

				final ServerInfo serverInfo = new ServerInfo();

				final XmlElement remoteIp = xml.getUnique(ipKey);
				serverInfo.setRemoteIp(remoteIp.getValue());

				final XmlElement localIps = xml.getUnique(localIpListKey);
				serverInfo.setLocalIps(Arrays.asList(localIps.getValue().split(",")));

				final XmlElement portXml = xml.getUnique(portKey);
				serverInfo.setHttpPort(Integer.parseInt(portXml.getValue()));

				if (xml.contains(httpsPortKey))
					serverInfo.setHttpsPort(Integer.parseInt(xml.getUnique(httpsPortKey).getValue()));

				if (xml.contains(certificateFingerprintKey))
					serverInfo.setCertificateFingerprint(xml.getUnique(certificateFingerprintKey).getValue());

				return serverInfo;
			});
	}
}
