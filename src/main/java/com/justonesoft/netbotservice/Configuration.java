package com.justonesoft.netbotservice;

public class Configuration {

	private Configuration() {}
	
	/* DEFAULT VALUES */
	public static final int HTTP_PORT_VALUE_DEFAULT = 8080;
	public static final int SOCKET_PORT_VALUE_DEFAULT = 9999;
	
	/* KEYS FOR CONFIGURATIONS */
	public static final String SOCKET_PORT = "Configuration.SOCKET_PORT";
	public static final String HTTP_PORT = "Configuration.HTTP_PORT";
	
	/* ARGUMENTS */
	public static final String ARG_SOCKET_PORT = "-SOCKET-PORT";
	public static final String ARG_HTTP_PORT = "-HTTP-PORT";
}
