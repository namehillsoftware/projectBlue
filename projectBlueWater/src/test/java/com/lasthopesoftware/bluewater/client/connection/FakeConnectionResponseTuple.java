package com.lasthopesoftware.bluewater.client.connection;

public class FakeConnectionResponseTuple {
	final int code;
	final byte[] response;

	public FakeConnectionResponseTuple(int code, byte[] response) {
		this.code = code;
		this.response = response;
	}
}
