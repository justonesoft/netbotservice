package com.justonesoft.netbotservice.sock.protocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.eclipse.jetty.util.BlockingArrayQueue;

/**
 * Receives a buffer and tries to read an image out of it
 * it might not have the whole image in the buffer
 * 	- must wait for the next buffer to come until completing the image
 * knows how many bytes the image has
 * 	- uses this to know when the image has been fully received
 * processes the whole buffer and checks if a new image is about to arrive
 */
public class ImageReader {
	
	private static ExecutorService service = Executors.newFixedThreadPool(10);// 10 threads in the pool
	
	private final BlockingQueue<byte[]> processQueue = new ArrayBlockingQueue<byte[]>(10);
	
	private Thread reader = new Thread(new Runnable() {
		
		public void run() {
			while (true) { // TODO maybe not true, leave room for terminating
				byte[] nextChunk = null;
				try {
					nextChunk = processQueue.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				processReadBuffer(nextChunk);
			}
			
		}
	});
	
	private final int IMAGE_LENGTH_BYTES = 4;
	private final int MAX_IMAGE_SIZE = 2 * 1024; // 2K bytes
	
	private ReadState state = ReadState.NONE;
	private int currentImageSize = 0;
	
	// we store the image length in one int aka 4 bytes
	private ByteBuffer imageSizeBuffer = ByteBuffer.allocate(IMAGE_LENGTH_BYTES);
	private ByteBuffer imageBuffer = ByteBuffer.allocate(MAX_IMAGE_SIZE);
	
	private Object readingLock = new Object();
	private volatile boolean readingOrClosed = false;
	
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
	
	public ImageReader() {
		reader.start();
	}
	
	public void readFromChannel(final SocketChannel sc) {
		try {
			if (!sc.isOpen()) return;
			
			final ByteBuffer readBB = ByteBuffer.allocate(10);
			final int bytesRead = sc.read(readBB);
	
			if (bytesRead == 0) return;
			
			if (bytesRead < 0) {
				// this socket channel has been closed
				System.out.println(Thread.currentThread().getName() + " closing channel " + sc);
				sc.close();
				return;
			}
			
			byte[] source = new byte[bytesRead];
			readBB.rewind();
			readBB.get(source);
			processQueue.put(source);
//			readBB.rewind();
//	
//			service.execute(new Runnable() {
//				
//				public void run() {
//					processReadBuffer(readBB, bytesRead);
//				}
//
//			});
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
				reset();
			} else {
				// put everything in the image buffer 
				byte[] remaining = new byte[availableBytes];
				readBB.get(remaining);
				imageBuffer.put(remaining);
			}
			break;
		}	
	}
	
	public void processReadBuffer (byte[] source) {
		// DO NOT FLIP THE BUFFER HERE
		
		switch (state) {
		
		case NONE:
			// should read the image size in 4 bytes
			
			if (source.length >= IMAGE_LENGTH_BYTES) {
				// have the full image size
				
				imageSizeBuffer.put(source, 0, 4);
				imageSizeBuffer.rewind();
				currentImageSize = imageSizeBuffer.getInt();
				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
				byte[] restCopy = new byte[source.length-4];
				System.arraycopy(source, 4, restCopy, 0, source.length-4);
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
				state = ReadState.IMAGE_INCOMPLETE; // we can now read the image data
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
				state = ReadState.NONE;
				saveAsFile();
				reset();
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
