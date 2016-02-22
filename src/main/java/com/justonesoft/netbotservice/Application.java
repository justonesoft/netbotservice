package com.justonesoft.netbotservice;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.justonesoft.netbotservice.rest.ControlPanel;
import com.justonesoft.netbotservice.rest.html.StaticPageHandler;

public class Application {
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		ResourceConfig config = new ResourceConfig(ControlPanel.class);
		config.registerInstances(new StaticPageHandler());
		
		URI baseUri = UriBuilder.fromUri("http://localhost").port(9998).build();
		
		Server server = JettyHttpContainerFactory.createServer(baseUri, config, false);
		
		server.start();
		
		try {
			server.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
