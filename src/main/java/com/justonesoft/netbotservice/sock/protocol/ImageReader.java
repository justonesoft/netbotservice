package com.justonesoft.netbotservice.sock.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;


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
	
	private final int PROCESSING_QUEUE_SIZE = 10;
	private final int CHUNK_BYTES_SIZE_TO_READ_FROM_CHANNEL = 64 * 1024; // how many bytes to try to read at once from SocketChannel
	final ByteBuffer readBB = ByteBuffer.allocate(CHUNK_BYTES_SIZE_TO_READ_FROM_CHANNEL);
	
	// we store the image length in one integer aka 4 bytes
	private ByteBuffer imageSizeBuffer = ByteBuffer.allocate(IMAGE_LENGTH_BYTES); // store the bytes composing the image size
	private ByteBuffer imageBuffer = null; // store the bytes composing the image
	private long start;
	
	private int timesRead = 0;
	/**
	 * After data is read from SocketChannel, the bytes will be added to this BlockingQueue.
	 */
	private final BlockingQueue<byte[]> processQueue = new LinkedBlockingQueue<byte[]>();
	
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

					nextChunk = processQueue.take(); // blocks until data is available or interrupted
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
		if (imageBuffer == null) return;

		byte[] imageData = new byte[currentImageSize];
		
		imageBuffer.rewind();
		imageBuffer.get(imageData);
		
		ImageReadyEvent event = new ImageReadyEvent(imageData);
		
		for (ImageReadyListener listener : imageReadyListeners) {
			listener.onImageReady(event);
		}
		
		long start  = System.currentTimeMillis();
//		System.out.println("TransformImageDuration: " + (System.currentTimeMillis() - start));
	}
	
	public void readFromChannel(final SocketChannel sc, Selector selector) throws ClosedChannelException {
		try {
			if (!sc.isOpen()) return;
			readBB.rewind();
			int bytesRead = sc.read(readBB);
			if (bytesRead == 0) return;
			
//			System.out.println("IntialBytesRead: " + bytesRead);
			
			if (bytesRead < 0) {
				// this socket channel has been closed
				sc.close();
				disconnected = true;
				reader.interrupt();
				throw new ClosedChannelException();
			}

			int totalRead = bytesRead;
			while ((bytesRead > 0 && totalRead < (CHUNK_BYTES_SIZE_TO_READ_FROM_CHANNEL - 1000))) {
				bytesRead = sc.read(readBB);
				totalRead += bytesRead;
			}
//			System.out.println("TotalBytesRead: " + totalRead);
			
			byte[] source = new byte[readBB.position()];
			readBB.rewind();
			readBB.get(source);
			
			processQueue.offer(source);
//			System.out.println("readFromChannelTime: " + System.currentTimeMillis());
		} catch (ClosedChannelException e) {
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} finally {
		}
	}
	
	public void processReadBuffer (byte[] source) {

		//System.out.println(state + " buffer size: " + source.length);
		if (source.length == 0) return;
		
		switch (state) {
		
		case NONE:
//			System.out.println("Start new frame");
//			start = System.currentTimeMillis();
			// should read the image size in 4 bytes
			
			if (source.length >= IMAGE_LENGTH_BYTES) {
				// have the full image size
				
				imageSizeBuffer.put(source, 0, IMAGE_LENGTH_BYTES);
				imageSizeBuffer.rewind();
				currentImageSize = imageSizeBuffer.getInt();
				System.out.println("Image size: " + currentImageSize);
				imageBuffer = ByteBuffer.allocate(currentImageSize);

				state = ReadState.FRAME_COUNT; // we can now read the image data
				
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
				state = ReadState.FRAME_COUNT; // we can now read the image data
				
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
		case FRAME_COUNT:
			if (source.length >= 1) {
				System.out.println("Frame: " + source[0]);
				state = ReadState.IMAGE_INCOMPLETE;
				
				if (source.length == 1) return; // no more data to read;
				
				byte[] restCopy = new byte[source.length-1];
				System.arraycopy(source, 1, restCopy, 0, source.length-1);
				processReadBuffer(restCopy);
			}
			break;
		case IMAGE_INCOMPLETE:
			int bytesNeededForCompleteImage = currentImageSize - imageBuffer.position();
			
			if (source.length >= bytesNeededForCompleteImage) {
				// we can read the whole image
				for (int i=0; i<bytesNeededForCompleteImage; i++) {
					imageBuffer.put(source[i]);
				}
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
	
	private void saveAsFile(byte[] imageData) {
		String fileName = "image_"+System.currentTimeMillis()+".jpg";
		try {
		
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			fos.write(imageData);
			fos.flush();
			fos.close();
		    
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void reset() {
		if (imageBuffer != null) {
			imageBuffer.rewind();
		}
		if (imageSizeBuffer != null) {
			imageSizeBuffer.rewind();
		}
		currentImageSize = 0;
	}
	
	ByteBuffer getImageData() {
		return imageBuffer;
	}
}
