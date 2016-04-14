package com.justonesoft.netbotservice.sock.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author bmunteanu
 * @Singleton
 */
public class DeviceRegistry {
	
	/* The constants preceded with CONF_ should pe put in config files.
	 * Their role is to configure the devices Map performance.
	 */
	private final int CONF_DEVICE_MAP_INITIAL_CAPACITY = 100; // Resizing a ConcurrentHashMap can be costly so tweak this number well.
	private final float CONF_DEVICE_MAP_LOAD_FACTOR = 0.75f;  // Has to do with when resizing will be done. When average number of elements per each bin in the Map hits this number.
	private final int CONF_DEVICE_MAP_CONCURRENCY_LEVEL = 16; // How many threads will perform updates on the Map. Can be set to the number of cores.
	
	private Map<String, List<Device>> registeredDevices = new ConcurrentHashMap<String, List<Device>>(
			CONF_DEVICE_MAP_INITIAL_CAPACITY,
			CONF_DEVICE_MAP_LOAD_FACTOR,
			CONF_DEVICE_MAP_CONCURRENCY_LEVEL); //TODO add an initial capacity
	
	private static DeviceRegistry instance = new DeviceRegistry();
	
	public static DeviceRegistry getInstance() {
		return instance;
	}
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
		
		devices = registeredDevices.get(owner);
		if (devices == null) {
			devices = new ArrayList<Device>(); //TODO add an initial capacity for list
			registeredDevices.put(owner, devices);
		}
		
		synchronized (devices) {
			if (!devices.contains(device)) {
				devices.add(device);
			}
		}
	}
	
	public void deregister(Device device) {
		if (device == null || device.getOwner() == null) {
			return;
		}
		
		List<Device> userDevices = registeredDevices.get(device.getOwner());
		if (userDevices != null) {
			synchronized (userDevices) {
				userDevices.remove(device);
				
				if (userDevices.isEmpty()) {
					registeredDevices.remove(device.getOwner());
				}
			}
		}
	}
	
	public List<Device> getDevicesList(String owner) {
		return registeredDevices.get(owner);
	}
}
