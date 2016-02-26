package com.justonesoft.netbotservice.sock.protocol;

/**
 * Triggered after a full image is ready from the SocketChannel.
 * 
 * @author bmunteanu
 * @ThreadSafe
 * @Immutable
 */
public class ImageReadyEvent {
	private final byte[] imageData;
	
	/**
	 * protected constructor
	 */
	ImageReadyEvent () {
		imageData = null;
	}
	
	public ImageReadyEvent(byte[] data) {
		this.imageData = data;
	}
	
	/**
	 * Return a copy of the internal buffer
	 * @return copy of internal data or empty array if internal data is null
	 */
	public byte[] getData() {
		
		if (imageData == null) {return new byte[0];}
		
		// return a copy
		byte[] copy = new byte[imageData.length];
		
		System.arraycopy(imageData, 0, copy, 0, imageData.length);
		
		return copy;
	}
}
