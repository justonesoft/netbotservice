package com.justonesoft.netbotservice.sock.protocol;

public enum ReadState {
	NONE,
	IMAGE_LENGTH,
	FRAME_COUNT,
	IMAGE_INCOMPLETE
}
