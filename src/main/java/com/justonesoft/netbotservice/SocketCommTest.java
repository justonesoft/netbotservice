package com.justonesoft.netbotservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

import com.justonesoft.netbotservice.sock.communication.BridgeSocketServer;

public class SocketCommTest {
	
	private static final int PORT = BridgeSocketServer.PORT;
	
	public static void main(String[] args) throws IOException {
		
		// prepare the bytes
		// will send 100 bytes of data plus 4 bytes as int representing the value 100 this is the ImageReader protocol
		int size = 100;
		ByteBuffer data = ByteBuffer.allocate(104);
		
		data.putInt(size);
		
		Random rand = new Random();
		byte[] byteData = new byte[size];
		
		rand.nextBytes(byteData);
		
		for (int i=0; i<size; i++) {
			System.out.print(byteData[i]+", ");
		}
		
		data.put(byteData);
		
		data.rewind();
		
		Socket clientSocket = new Socket();
		
		clientSocket.connect(new InetSocketAddress(PORT));
		
		byte[] firstFourBytes = new byte[4];
		data.get(firstFourBytes);
		clientSocket.getOutputStream().write(firstFourBytes);
		
		data.get(byteData);
		
		clientSocket.getOutputStream().write(byteData);
		
		clientSocket.getOutputStream().flush();
		
		clientSocket.getOutputStream().close();
		
		clientSocket.close();
	}
}
