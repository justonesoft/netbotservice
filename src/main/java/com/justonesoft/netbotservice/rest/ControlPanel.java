package com.justonesoft.netbotservice.rest;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.justonesoft.netbotservice.sock.protocol.Device;
import com.justonesoft.netbotservice.sock.protocol.DeviceRegistry;

@Path("/cp")
public class ControlPanel {
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String render() {
		return "Hello World";
	}
	
	@GET
	@Path("owner/{owner}/device/{device}/lastImage")
	@Produces("image/jpg")
	public byte[] lastImage(@PathParam("owner") String owner,
			@PathParam("device") String deviceName) {
		
		List<Device> devices = DeviceRegistry.getInstance().getDevicesList(owner);
		
		if (devices == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		for (Device device : devices) {
			if (device.getName().equals(deviceName)) {
				return device.getLastImage();
			}
		}
		
		throw new WebApplicationException(Status.NOT_FOUND);
	}

	@POST
	@Path("owner/{owner}/device/{device}/sendCommand")
	public void sendCommandFromQuery(@PathParam("owner") String owner,
			@PathParam("device") String deviceName, @QueryParam("type") byte type, @QueryParam("cmd") byte command) {
		
		List<Device> devices = DeviceRegistry.getInstance().getDevicesList(owner);
		
		if (devices == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		Device targetDevide = null;
		
		for (Device device : devices) {
			if (device.getName().equals(deviceName)) {
				targetDevide = device;
			}
		}
		
		if (targetDevide != null) {
			targetDevide.sendThis(type);
			targetDevide.sendThis(command);
		} else {
			System.out.println("Device not found");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}
}
