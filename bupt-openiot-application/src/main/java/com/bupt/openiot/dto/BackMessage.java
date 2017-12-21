package com.bupt.openiot.dto;

public class BackMessage {
	private String backCode;
	
	private String backMessage;
	
	private Object obj;

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	public String getBackMessage() {
		return backMessage;
	}

	public void setBackMessage(String backMessage) {
		this.backMessage = backMessage;
	}

	public String getBackCode() {
		return backCode;
	}

	public void setBackCode(String backCode) {
		this.backCode = backCode;
	}
	
}
