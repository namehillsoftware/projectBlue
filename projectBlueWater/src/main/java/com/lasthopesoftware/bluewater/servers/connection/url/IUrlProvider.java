package com.lasthopesoftware.bluewater.servers.connection.url;

/**
 * Created by david on 1/14/16.
 */
public interface IUrlProvider {
	String getUrl(String... params);
	String getBaseUrl();
}
