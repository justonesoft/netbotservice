package com.justonesoft.netbotservice.sock.protocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Receives a buffer and tries to read an image out of it
 * it might not have the whole image in the buffer
 * 	- must wait for the next buffer to come until completing the image
 * knows how many bytes the image has
 * 	- uses this to know when the image has been fully received
 * processes the whole buffer and checks if a new image is about to arrive
 */
public class ImageReader {
	private final int IMAGE_LENGTH_BYTES = 4; // 4 bytes / 1 integer
	private final int MAX_IMAGE_SIZE = 100 * 1024; // 100K bytes
	
	private final int PROCESSING_QUEUE_SIZE = 10;
	private final int CHUNK_BYTES_SIZE_TO_READ_FROM_CHANNEL = 1024; // how many bytes to try to read at once from SocketChannel
	
	// we store the image length in one integer aka 4 bytes
	private ByteBuffer imageSizeBuffer = ByteBuffer.allocate(IMAGE_LENGTH_BYTES); // store the bytes composing the image size
	private ByteBuffer imageBuffer = ByteBuffer.allocate(MAX_IMAGE_SIZE); // store the bytes composing the image

	/**
	 * After data is read from SocketChannel, the bytes will be added to this BlockingQueue.
	 */
	private final BlockingQueue<byte[]> processQueue = new ArrayBlockingQueue<byte[]>(PROCESSING_QUEUE_SIZE);
	
	/**
	 * All the listeners that need to be notified when an image is ready are stored here
	 */
	private List<ImageReadyListener> imageReadyListeners;
	
	/**
	 * TO signal the worker thread that the underlying socket is disconnected so that it stops the work.
	 */
	private boolean disconnected = false;
	
	/**
	 * This thread will read from the queue the bytes composing the image and will process them so that the full image can be received.
	 */
	private Thread reader = new Thread(new Runnable() {
		
		public void run() {
			while (!disconnected) { // TODO maybe not true, leave room for terminating
				byte[] nextChunk = null;
				try {

					nextChunk = processQueue.take(); // blocks until data is available or interupted
					processReadBuffer(nextChunk);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	});
	
	
	private ReadState state = ReadState.NONE;
	private int currentImageSize = 0;

	public ImageReader() {
		imageReadyListeners = new ArrayList<ImageReadyListener>();
		reader.start();
		reset();
	}
	
	/**
	 * 
	 * @param listener
	 * @NotThreadSafe
	 */
	public void registerImageReadyListener(ImageReadyListener listener) {
		this.imageReadyListeners.add(listener);
	}
	
	private void imageReady() {
		notifyImageReady();
	}
	
	private void notifyImageReady() {
		
		byte[] imageData = new byte[currentImageSize];
		
		imageBuffer.rewind();
		imageBuffer.get(imageData);
		
		ImageReadyEvent event = new ImageReadyEvent(imageData);
		
		for (ImageReadyListener listener : imageReadyListeners) {
			listener.onImageReady(event);
		}
	}
	
	public void readFromChannel(final SocketChannel sc) throws ClosedChannelException {
		try {
			if (!sc.isOpen()) return;
			
			final ByteBuffer readBB = ByteBuffer.allocate(CHUNK_BYTES_SIZE_TO_READ_FROM_CHANNEL);
			final int bytesRead = sc.read(readBB);
	
			if (bytesRead == 0) return;
			
			if (bytesRead < 0) {
				// this socket channel has been closed
				sc.close();
				disconnected = true;
				reader.interrupt();
				throw new ClosedChannelException();
			}
			
			byte[] source = new byte[bytesRead];
			readBB.rewind();
			readBB.get(source);
			
			processQueue.put(source);
		} catch (ClosedChannelException e) {
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
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
				processReadBuffer(readBB, availableBytes - IMAGE_LENGTH_BYTES);
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
				imageBuffer.put(remaining, 0, imageBuffer.position() + remaining.length > MAX_IMAGE_SIZE ? MAX_IMAGE_SIZE - imageBuffer.position() : remaining.length);
				state = ReadState.NONE;
				imageReady();
				reset();
			} else {
				// put everything in the image buffer 
				byte[] remaining = new byte[availableBytes];
				readBB.get(remaining, 0, imageBuffer.position() + remaining.length > MAX_IMAGE_SIZE ? MAX_IMAGE_SIZE - imageBuffer.position() : remaining.length);
				imageBuffer.put(remaining);
			}
			break;
		}	
	}
	
	public void processReadBuffer (byte[] source) {

		//System.out.println(state + " buffer size: " + source.length);
		if (source.length == 0) return;
		
		switch (state) {
		
		case NONE:
			// should read the image size in 4 bytes
			
			if (source.length >= IMAGE_LENGTH_BYTES) {
				// have the full image size
				
				imageSizeBuffer.put(source, 0, IMAGE_LENGTH_BYTES);
				imageSizeBuffer.rewind();
				currentImageSize = imageSizeBuffer.getInt();
				System.out.println("NONE - Image size: " + currentImageSize);
				imageBuffer = ByteBuffer.allocate(currentImageSize);

				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
				
				if (source.length == IMAGE_LENGTH_BYTES) return; // there is no more data left to process
				
				byte[] restCopy = new byte[source.length-IMAGE_LENGTH_BYTES];
				System.arraycopy(source, IMAGE_LENGTH_BYTES, restCopy, 0, source.length-IMAGE_LENGTH_BYTES);
				processReadBuffer(restCopy);
			} else {
				// not enough bytes to compose the image size
				// store what we have in internal buffer
				state = ReadState.IMAGE_LENGTH; // still need to read the image size
				
				for (int i=0; i<source.length; i++) {
					imageSizeBuffer.put(source[i]);
				}
			}
			break;
		case IMAGE_LENGTH:
			// continue to read the image length
			int bytesNeeded = IMAGE_LENGTH_BYTES - imageSizeBuffer.position();
			if (source.length >= bytesNeeded) {
				for (int i=0; i<bytesNeeded; i++) {
					imageSizeBuffer.put(source[i]);
				}
				currentImageSize = ((ByteBuffer)imageSizeBuffer.rewind()).getInt();
				System.out.println("IMAGE_LENGTH - Image size: " + currentImageSize);
				imageBuffer = ByteBuffer.allocate(currentImageSize);
				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
				
				if (source.length == bytesNeeded) return; // there is no more data left to process
				
				byte[] restCopy = new byte[source.length-bytesNeeded];
				System.arraycopy(source, bytesNeeded, restCopy, 0, source.length-bytesNeeded);
				processReadBuffer(restCopy);
			} else {
				for (int i=0; i<source.length; i++) {
					imageSizeBuffer.put(source[i]);
				}
			}
			break;
		case IMAGE_INCOMPLETE:
			int bytesNeededForCompleteImage = currentImageSize - imageBuffer.position();
			
			if (source.length >= bytesNeededForCompleteImage) {
				// we can read the whole image
				for (int i=0; i<bytesNeededForCompleteImage; i++) {
					imageBuffer.put(source[i]);
				}
				System.out.println("Image ready: " + imageBuffer.position() + " / " + imageBuffer.limit());
				state = ReadState.NONE;
				imageReady();
				reset();
				if (source.length > bytesNeededForCompleteImage) {
					// there is more data left to process, from the next picture probably
					byte[] restCopy = new byte[source.length-bytesNeededForCompleteImage];
					System.arraycopy(source, bytesNeededForCompleteImage, restCopy, 0, source.length-bytesNeededForCompleteImage);
					processReadBuffer(restCopy);
				}
			} else {
				// put everything in the image buffer 
				for (int i=0; i<source.length; i++) {
					imageBuffer.put(source[i]);
				}
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
				byte b = imageBuffer.get();
				fos.write(b);
				System.out.print(b + ", ");
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
	
	private void reset() {
		imageBuffer.rewind();
		imageSizeBuffer.rewind();
		currentImageSize = 0;
	}
	
	ByteBuffer getImageData() {
		return imageBuffer;
	}
}
