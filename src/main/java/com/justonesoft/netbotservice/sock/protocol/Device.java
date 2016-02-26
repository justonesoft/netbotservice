package com.justonesoft.netbotservice.sock.protocol;

public class Device {
	private String name;
	private String owner;
	
	private long lastConnected; //timestamp
	private long lastAction; // timestamp
	
	private byte[] lastImage; // or store it to a file and keep the file name/location
	
	public Device() {
		
	}
	
	@Override
	public boolean equals(Object otherDeviceObj) {
		if (this == otherDeviceObj) return true;

		if (otherDeviceObj == null) return false;
		
		if ( !(otherDeviceObj instanceof Device) ) return false;
		
		Device otherDevice = (Device) otherDeviceObj;
		
		if (this.name == null && otherDevice.getName() != null) return false;
		if (this.owner == null && otherDevice.getOwner() != null) return false;
		
		return this.name.equals(otherDevice.getName()) && 
				this.owner.equals(otherDevice.getOwner());
	}
	
	public Device(String name, String owner) {
		this.name = name;
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public long getLastConnected() {
		return lastConnected;
	}

	public void setLastConnected(long lastConnected) {
		this.lastConnected = lastConnected;
	}

	public long getLastAction() {
		return lastAction;
	}

	public void setLastAction(long lastAction) {
		this.lastAction = lastAction;
	}

	public byte[] getLastImage() {
		return lastImage;
	}

	public void setLastImage(byte[] lastImage) {
		this.lastImage = lastImage;
	}
}
