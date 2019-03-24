package org.outreach.outreachfeedbackserver.model;

public enum Roles {
	
	ADMIN("A"),PMO("PMO"),POC("POC");
	
	String code;
	
	private Roles(String code) {
		this.code=code;
	}
	
	public String getCode() {
		return this.code;
	}
}
