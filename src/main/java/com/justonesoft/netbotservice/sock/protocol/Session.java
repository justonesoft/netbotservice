package com.justonesoft.netbotservice.sock.protocol;

public class Session {
	private SessionState state;
	private ReaderManager reader;
	
	public Session() {
		this.state = SessionState.ANONYMOUS;
		this.reader = new ReaderManager();
	}
	
	public void connected() {
		this.state = SessionState.CONNECTED;
	}
}
