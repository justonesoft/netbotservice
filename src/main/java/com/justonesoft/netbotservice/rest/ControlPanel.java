package com.justonesoft.netbotservice.rest;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.drew.metadata.MetadataException;
import com.justonesoft.netbotservice.sock.protocol.Device;
import com.justonesoft.netbotservice.sock.protocol.DeviceRegistry;

@Path("/cp")
public class ControlPanel {
	private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";

    private static final String HTTP_HEADER =
        "HTTP/1.0 200 OK\r\n"
        + "Server: gizmo-hub\r\n"
        + "Connection: close\r\n"
        + "Max-Age: 0\r\n"
        + "Expires: 0\r\n"
        + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, "
            + "post-check=0, max-age=0\r\n"
        + "Pragma: no-cache\r\n"
        + "Access-Control-Allow-Origin:*\r\n"
        + "Content-Type: multipart/x-mixed-replace; "
            + "boundary=" + BOUNDARY + "\r\n"
        + BOUNDARY_LINES;
	
    private Map<String, String> map = new HashMap<String, String>();
    
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
		
	    return lastImage;
	}
	
	@GET
	@Path("owner/{owner}/device/{device}/stream/{streamId}/lastImage")
	@Produces("image/jpg")
	public StreamingOutput streamLastImage(final @PathParam("streamId") String streamId, @PathParam("owner") String owner,
			@PathParam("device") String deviceName) throws MetadataException {
		
		List<Device> devices = DeviceRegistry.getInstance().getDevicesList(owner);
		
		if (devices == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		Device device = null;
		for (Device deviceInList : devices) {
			if (deviceInList.getName().equals(deviceName)) {
				device = deviceInList;
			}
		}
		if (device == null) {
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
		final String fromMapStreamId = map.get(streamId);
		final Device finalDevice = device;
		return new StreamingOutput() {
			
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {

					DataOutputStream dos = new DataOutputStream(output);
					if (fromMapStreamId == null) {
						System.out.println("StreamId: " + streamId + " not found");
						dos.writeBytes(HTTP_HEADER);
						dos.flush();
						System.out.println("ONE");
						map.put(streamId, streamId);
					} 
					else {
						System.out.println("found streamId: " + streamId);
					}
					long now = System.currentTimeMillis();
					long end = now + (30 * 1000);
					while (now < end) {
						dos.writeBytes(
				                "Content-type: image/jpeg\r\n"
				                + "Content-Length: " + finalDevice.getLastImage().length + "\r\n"
				                + "X-Timestamp:" + now + "\r\n"
				                + "\r\n"
				            );
						dos.write(finalDevice.getLastImage(), 0 /* offset */, finalDevice.getLastImage().length);
						dos.writeBytes(BOUNDARY_LINES);
						dos.flush();
						System.out.println("TWO");
						now = System.currentTimeMillis();
					}
					dos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

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
