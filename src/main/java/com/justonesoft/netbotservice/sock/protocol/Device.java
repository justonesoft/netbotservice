package com.justonesoft.netbotservice.sock.protocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Device implements ImageReadyListener {
	private String name;
	private String owner;
	
	private long lastConnected; //timestamp
	private long lastAction; // timestamp
	
	private byte[] lastImage; // or store it to a file and keep the file name/location
	
	private final ImageReader imageReader;

	private final BlockingQueue<Byte> writingQueue = new ArrayBlockingQueue<Byte>(10);

	public Device(SocketChannel sc) {
		this();
		
		writeToChannel(sc);
	}
	public Device() {
		this.name = "N/A";
		this.owner = "ANONYMOUS";
		
		imageReader = new ImageReader();
		imageReader.registerImageReadyListener(this);
	}
	
	public Device(String name, String owner) {
		this.name = name;
		this.owner = owner;

		imageReader = new ImageReader();
		imageReader.registerImageReadyListener(this);
	}

	public void readFromChannel(final SocketChannel sc) {
		try {
			imageReader.readFromChannel(sc);
		} catch (ClosedChannelException e) {
			System.out.println("readFromChannel - disconnected");
			DeviceRegistry.getInstance().deregister(this);
		}
	}
	
	public void writeToChannel(final SocketChannel sc) {
		// TODO use a Thread Manager
		new Thread(new Runnable() {
			
			public void run() {
				ByteBuffer buffer = ByteBuffer.allocate(1);
				while (sc.isConnected()) {
					try {
						System.out.println("Waiting for data to write");
						Byte dataToSend = writingQueue.poll(3, TimeUnit.SECONDS); // wake up after 3 seconds, this is so that does not stuck here if socket is closed
						
						if (dataToSend == null) continue;
						
						System.out.println("Writing " + dataToSend.intValue());
						buffer.put(dataToSend.byteValue());
						buffer.rewind();
						while (buffer.hasRemaining()) {
							sc.write(buffer);
						}
						buffer.rewind();

					} catch (ClosedChannelException e) {
						// return to the while condition that does not hold anymore and exit the loop and thread
						System.out.println("writeToChannel - disconnected");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}).start();
	}
	
	public boolean sendThis(byte whatToWrite) {
		System.out.println("Submiting " + whatToWrite + " for writing");
		return writingQueue.offer(Byte.valueOf(whatToWrite)); // this will block if nothing consumes the added bytes. for example if SocketChannel gets disconnected
		// TODO there is still a problem with this
		// even if socket gets disconnected this method would not know about and still add data into the queue
	}
	
	/**
	 * @NotThreadSafe access to lastImage is not synchronized
	 */
	public void onImageReady(ImageReadyEvent event) {
		lastImage = event.getData();
//		saveAsFile();
	}

	@Override
	public boolean equals(Object otherDeviceObj) {
		if (this == otherDeviceObj) return true;

		if (otherDeviceObj == null) return false;
		
		if ( !(otherDeviceObj instanceof Device) ) return false;
		
		Device otherDevice = (Device) otherDeviceObj;
		
		if (this.name == null && otherDevice.getName() != null) return false;
		if (this.owner == null && otherDevice.getOwner() != null) return false;
		
		return this.name.equals(otherDevice.getName()) && 
				this.owner.equals(otherDevice.getOwner());
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public long getLastConnected() {
		return lastConnected;
	}

	public void setLastConnected(long lastConnected) {
		this.lastConnected = lastConnected;
	}

	public long getLastAction() {
		return lastAction;
	}

	public void setLastAction(long lastAction) {
		this.lastAction = lastAction;
	}

	public byte[] getLastImage() {
		return lastImage;
	}

	public void setLastImage(byte[] lastImage) {
		this.lastImage = lastImage;
	}

	/**
	 * @NotThreadSafe access to lastImage is not synchronized, lastImage can change in onImageReady during execution of this method
	 */
	private void saveAsFile() {
		String fileName = "image_"+System.currentTimeMillis()+".jpg";
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			
			for (int i=0; i<lastImage.length; i++) {
				byte b = lastImage[i];
				fos.write(b);
				System.out.print(b + ", ");
			}
			
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
}
