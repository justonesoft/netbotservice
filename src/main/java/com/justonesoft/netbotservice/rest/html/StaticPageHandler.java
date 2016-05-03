package com.justonesoft.netbotservice.rest.html;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/static")
@Singleton
public class StaticPageHandler {
	
	private final String CONTEXT = "web";
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("{file}")
	public Response handleStaticHtml(@PathParam("file") @DefaultValue("index.html") String fileName) {
		
		File fileLocation = new File(CONTEXT, fileName);
		
		if (!fileLocation.exists()) {
			
			URL fileUri = Thread.currentThread().getContextClassLoader().getResource(fileName);
			fileLocation = new File(fileUri.getFile());
		}
		
		return Response.ok(fileLocation).build();
		
	}
}
