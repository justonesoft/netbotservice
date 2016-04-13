package com.justonesoft.netbotservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

import com.justonesoft.netbotservice.sock.communication.BridgeSocketServer;

public class SocketCommTest {
	
	private static final int PORT = BridgeSocketServer.SOCKET_SERVER_PORT;
	
	private static final String sampleName = "pom.xml";
	
	public static void main(String[] args) throws IOException {
		
		File sampleFile = new File(sampleName);
		
		int size = (int)sampleFile.length();
		
		byte[] byteData = new byte[size];
		
		// prepare the bytes
		// will send 100 bytes of data plus 4 bytes as int representing the value 100 this is the ImageReader protocol
		ByteBuffer data = ByteBuffer.allocate((int)size+4);
		
		data.putInt(size);
		
		FileInputStream fis = new FileInputStream(sampleFile);
		
		fis.read(byteData);

		fis.close();
		
		data.put(byteData);
		
		data.rewind();
		
		Socket clientSocket = new Socket();
		
		clientSocket.connect(new InetSocketAddress( "52.26.154.254", PORT));
		
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
