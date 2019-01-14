package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import xmlwise.XmlElement;

import java.util.Arrays;

public class ServerLookup implements LookupServers {

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

				final XmlElement remoteIp = xml.getUnique("ip");
				if (remoteIp != null) serverInfo.setRemoteIp(remoteIp.getValue());

				final XmlElement localIps = xml.getUnique("localiplist");
				if (localIps != null) serverInfo.setLocalIps(Arrays.asList(localIps.getValue().split(",")));

				final XmlElement portXml = xml.getUnique("port");
				if (portXml != null) serverInfo.setHttpPort(Integer.parseInt(portXml.getValue()));

				final XmlElement securePortXml = xml.getUnique("https_port");
				if (securePortXml != null) serverInfo.setHttpsPort(Integer.parseInt(securePortXml.getValue()));

				final XmlElement certificateFingerprintXml = xml.getUnique("certificate_fingerprint");
				if (certificateFingerprintXml != null) serverInfo.setCertificateFingerprint(certificateFingerprintXml.getValue());

				return serverInfo;
			});
	}
}
