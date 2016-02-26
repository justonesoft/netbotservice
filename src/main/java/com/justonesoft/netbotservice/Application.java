package com.justonesoft.netbotservice;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.justonesoft.netbotservice.rest.ControlPanel;
import com.justonesoft.netbotservice.rest.html.StaticPageHandler;
import com.justonesoft.netbotservice.sock.communication.BridgeSocketServer;

public class Application {
	public static final int HTTP_SERVER_PORT = 9998;
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		ResourceConfig config = new ResourceConfig(ControlPanel.class);
		config.registerInstances(new StaticPageHandler());
		
		URI baseUri = UriBuilder.fromUri("http://localhost").port(HTTP_SERVER_PORT).build();
		
		Server server = JettyHttpContainerFactory.createServer(baseUri, config, false);
		
		server.start();
		
		BridgeSocketServer bridge = new BridgeSocketServer();
		bridge.start();

		try {
			server.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
