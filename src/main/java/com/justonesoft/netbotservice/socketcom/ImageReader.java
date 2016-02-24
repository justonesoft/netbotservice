package com.justonesoft.netbotservice.socketcom;

public class ImageReader implements ClosedChannelListener {

	public void onChannelClosed() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * receives a buffer and tries to read an image out of it
	 * it might not have the whole image in the buffer
	 * 	- must wait for the next buffer to come until completing the image
	 * knows how many bytes the image has
	 * 	- uses this to know when the image has been fully received
	 * processes the whole buffer and checks if a new image is about to arrive
	 */
}
