package com.justonesoft.netbotservice.sock.protocol;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.justonesoft.netbotservice.sock.protocol.ImageReader;


public class ImageReaderTest {
	
	@Test
	public void testReadFullImage() {
		int size = 4;
		String imageData = "abcd";
		
		// create a bytebuffer
		ByteBuffer src = ByteBuffer.allocate(4 + imageData.length());
		src.putInt(size);
		src.put(imageData.getBytes());
		
		src.flip();
		
		ImageReader rm = new ImageReader();
		
		rm.processReadBuffer(src, 4 + imageData.length());
		
		ByteBuffer image = rm.getImageData();
		
		byte[] dst = new byte[imageData.length()];
		
		image.rewind();
		
		image.get(dst);
		
		Assert.assertEquals(imageData, new String(dst));
	}

	@Test
	public void testReadPartialImageSize() {
		int size = 4;
		String imageData = "abcd";
		
		ByteBuffer partialBuffer = ByteBuffer.allocate(4);
		partialBuffer.putInt(size);
		partialBuffer.rewind();

		ImageReader rm = new ImageReader();
		
		// split the image size part in 1 + 1+ 2 bytes
		rm.processReadBuffer(partialBuffer, 1);
		rm.processReadBuffer(partialBuffer, 1);
		rm.processReadBuffer(partialBuffer, 2);

		// now send the real data
		
		// create a bytebuffer
		ByteBuffer src = ByteBuffer.allocate(imageData.length());
		src.put(imageData.getBytes());
		src.rewind();
		
		
		rm.processReadBuffer(src, imageData.length());
		
		ByteBuffer image = rm.getImageData();
		
		byte[] dst = new byte[size];
		
		image.rewind();
		
		image.get(dst);
		
		Assert.assertEquals(imageData, new String(dst));
	}

	@Test
	public void testReadPartialImageData() {
		int size = 4;
		String imageData1 = "ab";
		String imageData2 = "cd";
		
		ByteBuffer partialBuffer = ByteBuffer.allocate(4 + imageData1.length());
		partialBuffer.putInt(size);
		partialBuffer.put(imageData1.getBytes());
		partialBuffer.rewind();

		ImageReader rm = new ImageReader();
		
		// split the image size part in 1 + 1+ 2 bytes
		rm.processReadBuffer(partialBuffer, 4+imageData1.length());

		// now send the real data
		
		// create a bytebuffer
		ByteBuffer nextData = ByteBuffer.allocate(imageData2.length());
		nextData.put(imageData2.getBytes());
		nextData.rewind();
		
		
		rm.processReadBuffer(nextData, imageData2.length());
		
		ByteBuffer image = rm.getImageData();
		
		byte[] dst = new byte[size];
		
		image.rewind();
		
		image.get(dst);
		
		Assert.assertEquals(imageData1+imageData2, new String(dst));
	}
}
