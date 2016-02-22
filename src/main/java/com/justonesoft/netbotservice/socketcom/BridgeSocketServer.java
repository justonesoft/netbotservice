package com.justonesoft.netbotservice.socketcom;

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

public class BridgeSocketServer {
	public static final int PORT = 9999;
	
	// we need to have a pool of threads to allocate for read/write operations
	// the select operation will happen on a separate thread
	
	public static void main(String[] args) throws IOException {
		
		String message = "I got your request";
		ByteBuffer readBB = ByteBuffer.allocate(1024);
		ByteBuffer writeBB = ByteBuffer.allocate(message.length());
		
		writeBB.put(message.getBytes());
		

		Selector selector = Selector.open();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking( false );
		
		ServerSocket ss = ssc.socket();
		
		InetSocketAddress address = new InetSocketAddress( PORT );
		ss.bind( address );
		
		SelectionKey sscKey = ssc.register( selector, SelectionKey.OP_ACCEPT );
		
		for (int z=0; z<10; z++) {
			int num = selector.select();
			
			System.out.println("Run: " + z + "Selected: " + num);
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			
			Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
			
			while (keysIterator.hasNext()) {
				SelectionKey key =  keysIterator.next();
				keysIterator.remove();
				
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
					SocketChannel sc = ssc.accept();
					
					sc.configureBlocking( false );
					SelectionKey newKey = sc.register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					
					System.out.println("Run: "+z+"ACCEPT");
				} 
				if ((key.readyOps() & SelectionKey.OP_READ)
						== SelectionKey.OP_READ) {
					// Read the data
					SocketChannel sc = (SocketChannel)key.channel();
					// ...
					int bytesRead = sc.read(readBB);
					
					readBB.rewind();
					System.out.println("Run: "+z+" READ: " + bytesRead);
					for (int i=0; i < bytesRead; i++)
					{
						System.out.print((char)readBB.get());
					}
				}
				
				if ((key.readyOps() & SelectionKey.OP_WRITE)
						== SelectionKey.OP_WRITE) {
					// Read the data
					SocketChannel sc = (SocketChannel)key.channel();
					
					System.out.println("Run: "+z+" WRITE: ");
					
					writeBB.rewind();
					
					sc.write(writeBB);
					
					
					sc.close();

				}

			}
		}

	}
}
