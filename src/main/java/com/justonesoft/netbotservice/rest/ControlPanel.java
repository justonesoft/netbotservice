package com.justonesoft.netbotservice.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/cp")
public class ControlPanel {
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String render() {
		return "Hello World";
	}
}
