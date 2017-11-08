package com.namehillsoftware.handoff;


public class Message<Resolution> {
	public final Resolution resolution;
	public final Throwable rejection;

	Message(Resolution resolution, Throwable rejection) {
		this.resolution = resolution;
		this.rejection = rejection;
	}
}
