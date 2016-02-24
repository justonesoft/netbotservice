package com.justonesoft.netbotservice.socketcom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ReaderManager {
	private final int IMAGE_LENGTH_BYTES = 4;
	private final int MAX_IMAGE_SIZE = 2 * 1024; // 2K bytes
	
	private ReadState state = ReadState.NONE;
	private int currentImageSize = 0;
	
	// we store the image length in one int aka 4 bytes
	private ByteBuffer imageSizeBuffer = ByteBuffer.allocate(IMAGE_LENGTH_BYTES);
	private ByteBuffer imageBuffer = ByteBuffer.allocate(MAX_IMAGE_SIZE);
	
	/**
	 * All the read operations go through this entity
	 * Knows to handle all types of data:
	 * 	- images
	 *  - responses
	 *  - text messages
	 *  - sensor value
	 *  
	 * 
	 */
	
	public void readFromChannel(SocketChannel sc) {
		ByteBuffer readBB = ByteBuffer.allocate(1024);

		try {
			int bytesRead = sc.read(readBB);
			readBB.rewind();
			
			processReadBuffer(readBB, bytesRead);
			readBB.rewind();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void processReadBuffer (ByteBuffer readBB, int availableBytes) {
		// DO NOT FLIP THE BUFFER HERE
		
		switch (state) {
		
		case NONE:
			// should read the image size in 4 bytes
			imageSizeBuffer.rewind(); //reset the buffer
			
			if (availableBytes >= IMAGE_LENGTH_BYTES) {
				// have the full image size
				currentImageSize = readBB.getInt();
				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
				processReadBuffer(readBB, availableBytes - 4);
			} else {
				// not enough bytes to compose the image size
				// store what we have in internal buffer
				state = ReadState.IMAGE_LENGTH; // still need to read the image size
				
				for (int i=0; i<availableBytes; i++) {
					imageSizeBuffer.put(readBB.get());
				}
			}
			break;
		case IMAGE_LENGTH:
			// continue to read the image length
			int bytesNeeded = IMAGE_LENGTH_BYTES - imageSizeBuffer.position();
			if (availableBytes >= bytesNeeded) {
				for (int i=0; i<bytesNeeded; i++) {
					imageSizeBuffer.put(readBB.get());
				}
				currentImageSize = ((ByteBuffer)imageSizeBuffer.flip()).getInt();
				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
				processReadBuffer(readBB, availableBytes - bytesNeeded);
			} else {
				for (int i=0; i<availableBytes; i++) {
					imageSizeBuffer.put(readBB.get());
				}
			}
			break;
		case IMAGE_INCOMPLETE:
			int bytesNeededForCompleteImage = currentImageSize - imageBuffer.position();
			if (availableBytes >= bytesNeededForCompleteImage) {
				// we can read the whole image
				byte[] remaining = new byte[bytesNeededForCompleteImage];
				readBB.get(remaining);
				imageBuffer.put(remaining);
				state = ReadState.NONE;
				saveAsFile();
			} else {
				// put everything in the image buffer 
				byte[] remaining = new byte[availableBytes];
				readBB.get(remaining);
				imageBuffer.put(remaining);
			}
			break;
		}	
	}
	
	private void saveAsFile() {
		String fileName = "image_"+System.currentTimeMillis()+".jpg";
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			
			imageBuffer.rewind();
			for (int i=0; i<currentImageSize; i++) {
				fos.write(imageBuffer.get());
			}
			
			fos.flush();
			fos.close();
			
			imageBuffer.rewind();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	ByteBuffer getImageData() {
		return imageBuffer;
	}
}
