package com.justonesoft.netbotservice.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.drew.metadata.MetadataException;
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
			@PathParam("device") String deviceName) throws MetadataException {
		
		List<Device> devices = DeviceRegistry.getInstance().getDevicesList(owner);
		
		if (devices == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		byte[] lastImage = null;
		for (Device device : devices) {
			if (device.getName().equals(deviceName)) {
				lastImage = device.getLastImage();
			}
		}
		if (lastImage == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
//		try {
//			Metadata metadata = JpegMetadataReader.readMetadata(new ByteArrayInputStream(lastImage));
//			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//			if (directory != null) {
//				int orientation = directory.getInt(ExifSubIFDDirectory.TAG_ORIENTATION);
//				System.out.println("orientation: " + orientation);
//			}
//		} catch (JpegProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
//		}
		return lastImage;
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
			if (targetDevide.sendThis(type)) { // don't send the command if the type could not be sent
				targetDevide.sendThis(command); // still flawed if this can not be sent, next "type"
												// will actually be considered command and we desync with
												// client
			}
		} else {
			System.out.println("Device not found");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}
}
