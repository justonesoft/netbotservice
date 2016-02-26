package com.justonesoft.netbotservice.sock.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author bmunteanu
 *
 */
public class DeviceRegistry {
	
	private static Map<String, List<Device>> registeredDevices = new HashMap<String, List<Device>>(); //TODO add an initial capacity
	
	/**
	 * Add a device to the registered devices. <br />
	 * If the device is already in the list will not be added again.
	 * 
	 * @ThreadSafe
	 * @param device The device to be registered
	 */
	public void register(Device device) {
		if (device == null || device.getOwner() == null) {
			return;
		}
		
		String owner = device.getOwner();

		List<Device> devices = registeredDevices.get(owner);

		synchronized (registeredDevices) {
			devices = registeredDevices.get(owner);
			if (devices == null) {
				devices = new ArrayList<Device>(); //TODO add an initial capacity for list
				registeredDevices.put(owner, devices);
			}
		}
		
		synchronized (devices) {
			if (!devices.contains(device)) {
				devices.add(device);
			}
		}
	}
}
