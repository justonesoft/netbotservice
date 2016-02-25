package com.justonesoft.netbotservice.sock.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.justonesoft.netbotservice.sock.protocol.ImageReader;

/*
 * Protocol for receiving images:
 * start-char: #
 * next-long-imagesize-in-bytes: long
 * next-image-bytes: byte[]
 */
public class BridgeSocketServer extends Thread {
	public static final int PORT = 9999;
	private Selector selector;
	private ImageReader readerManager = new ImageReader();
	
	// we need to have a pool of threads to allocate for read/write operations
	// the select operation will happen on a separate thread
	
	public BridgeSocketServer() throws IOException {
		
		String message = "I got your request";
		ByteBuffer writeBB = ByteBuffer.allocate(message.length());
		
		writeBB.put(message.getBytes());
		

		selector = Selector.open();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking( false );
		
		InetSocketAddress address = new InetSocketAddress( PORT );
		ssc.bind( address );
		
		ssc.register( selector, SelectionKey.OP_ACCEPT );
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		BridgeSocketServer bridge = new BridgeSocketServer();
		bridge.start();
		bridge.join();
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				// blocking
				selector.select();
				
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				
				Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
				
				while (keysIterator.hasNext()) {
					SelectionKey key =  keysIterator.next();
					keysIterator.remove();
					
					if (!key.isValid()) continue;
					
					if (key.isAcceptable()) {
						ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
						SocketChannel sc = serverChannel.accept();
						
						sc.configureBlocking( false );
						sc.register( selector, SelectionKey.OP_READ, new ImageReader());
						
						System.out.println(Thread.currentThread().getName() + " accepted client");
					} 
					
					if (key.isValid() && key.isReadable()) {
						// Read the data
						SocketChannel sc = (SocketChannel)key.channel();
						ImageReader rm = (ImageReader) key.attachment();

						
						System.out.println(Thread.currentThread().getName() + " start reading thread.");
						/*
						 * Start a thread to read from SC
						 * a Reader is a thread that reads from socketChannel into the device internal data
						 * 
						 */
						rm.readFromChannel(sc);
						
						System.out.println(Thread.currentThread().getName() + " return to loop after starting reading thread");
					}
					
					if (key.isValid() && key.isReadable()) {
						// Read the data
//						SocketChannel sc = (SocketChannel)key.channel();
//						keysIterator.remove();
//						
//						System.out.println("Run: "+z+" WRITE: ");
//						
//						writeBB.rewind();
//						
//						sc.write(writeBB);
//						
//						
//						sc.close();

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
