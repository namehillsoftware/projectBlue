package com.lasthopesoftware.promises;


class Message<Resolution> {
	final Resolution resolution;
	final Throwable rejection;

	Message(Resolution resolution, Throwable rejection) {
		this.resolution = resolution;
		this.rejection = rejection;
	}
}
