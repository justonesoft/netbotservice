package com.justonesoft.netbotservice.sock.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
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
//				System.out.println("selector.select()-time: " + System.currentTimeMillis());
				
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
						System.out.println("RcvBuffSize: " + sc.getOption(StandardSocketOptions.SO_RCVBUF));
						System.out.println("TcpNodelay-before: " + sc.getOption(StandardSocketOptions.TCP_NODELAY));
						sc.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
						System.out.println("TcpNodelay-before: " + sc.getOption(StandardSocketOptions.TCP_NODELAY));
//						sc.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
						Device device = new Device(sc);
						
						System.out.println(Thread.currentThread().getName() + " register socket.");
						DeviceRegistry.getInstance().register(device);
						sc.register( selector, SelectionKey.OP_READ, device);
						
					} 
					
					if (key.isValid() && key.isReadable()) {
						// Read the data
						SocketChannel sc = (SocketChannel)key.channel();
						Device device = (Device) key.attachment();

						
//						System.out.println(Thread.currentThread().getName() + " start reading thread.");
						device.readFromChannel(sc, selector);
					}
					
//					if (key.isValid() && key.isWritable()) {
//						// Read the data
//						SocketChannel sc = (SocketChannel)key.channel();
//						Device device = (Device) key.attachment();
//						
//						System.out.println(" WRITE: ");
//						device.writeToChannel(sc);
//					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
