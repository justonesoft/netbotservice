package com.justonesoft.netbotservice.sock.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.justonesoft.netbotservice.sock.protocol.Device;
import com.justonesoft.netbotservice.sock.protocol.DeviceRegistry;

public class BridgeSocketServer extends Thread {
	public static final int SOCKET_SERVER_PORT = 9999;
	
	private Selector selector;
	
	public BridgeSocketServer() throws IOException {
		
		selector = Selector.open();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking( false );
		
		InetSocketAddress address = new InetSocketAddress( SOCKET_SERVER_PORT );
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
						
						Device device = new Device();
						System.out.println(Thread.currentThread().getName() + " register socket.");
						DeviceRegistry.getInstance().register(device);
						sc.register( selector, SelectionKey.OP_READ, device);
						
					} 
					
					if (key.isValid() && key.isReadable()) {
						// Read the data
						SocketChannel sc = (SocketChannel)key.channel();
						Device device = (Device) key.attachment();

						
//						System.out.println(Thread.currentThread().getName() + " start reading thread.");
						device.readFromChannel(sc);
					}
					
					if (key.isValid() && key.isWritable()) {
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
