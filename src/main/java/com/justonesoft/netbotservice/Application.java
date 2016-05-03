package com.justonesoft.netbotservice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.justonesoft.netbotservice.rest.ControlPanel;
import com.justonesoft.netbotservice.rest.html.StaticPageHandler;
import com.justonesoft.netbotservice.sock.communication.BridgeSocketServer;

public class Application {
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		Map<String, Object> confMap = parseArguments(args); 
				
		ResourceConfig config = new ResourceConfig(ControlPanel.class);
		config.registerInstances(new StaticPageHandler());
		
		URI baseUri = UriBuilder.fromUri("http://localhost").port((Integer)confMap.get(Configuration.HTTP_PORT)).build();
		
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
	
	private static Map<String, Object> parseArguments(String[] args) {
		// set defaults
		Map<String, Object> argsMap = new HashMap<String, Object>();
		argsMap.put(Configuration.SOCKET_PORT, Configuration.SOCKET_PORT_VALUE_DEFAULT);
		argsMap.put(Configuration.HTTP_PORT, Configuration.HTTP_PORT_VALUE_DEFAULT);
		
		for (String arg : args) {
			System.out.println("Loading argument: " + arg + ":");
			if (arg.contains(Configuration.ARG_SOCKET_PORT)) {
				setConfigFromArg(argsMap, Configuration.SOCKET_PORT, arg);
			}
			else if (arg.contains(Configuration.ARG_HTTP_PORT)) {
				setConfigFromArg(argsMap, Configuration.HTTP_PORT, arg);
			}
		}
		return argsMap;
	}

	private static void setConfigFromArg(Map<String, Object> argsMap, String confKey, String arg) {
		String argValue = arg.substring(arg.indexOf("=")+1);
		System.out.println(confKey + " : " + argValue);
		argsMap.put(confKey, Integer.parseInt(argValue));
	}
}
