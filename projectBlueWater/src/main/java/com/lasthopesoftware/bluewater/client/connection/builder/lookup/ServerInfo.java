package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import java.util.Collection;

public class ServerInfo {
	private int httpPort;
	private Integer httpsPort;
	private String remoteIp;
	private Collection<String> localIps;

	public Collection<String> getLocalIps() {
		return localIps;
	}

	public ServerInfo setLocalIps(Collection<String> localIps) {
		this.localIps = localIps;
		return this;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public ServerInfo setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
		return this;
	}

	public Integer getHttpsPort() {
		return httpsPort;
	}

	public ServerInfo setHttpsPort(Integer httpsPort) {
		this.httpsPort = httpsPort;
		return this;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public ServerInfo setHttpPort(int httpPort) {
		this.httpPort = httpPort;
		return this;
	}
}
