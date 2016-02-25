package com.justonesoft.netbotservice.sock.protocol;

public class Session {
	private SessionState state;
	private ImageReader reader;
	
	public Session() {
		this.state = SessionState.ANONYMOUS;
		this.reader = new ImageReader();
	}
	
	public void connected() {
		this.state = SessionState.CONNECTED;
	}
}
