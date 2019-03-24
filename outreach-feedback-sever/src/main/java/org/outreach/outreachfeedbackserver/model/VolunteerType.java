package org.outreach.outreachfeedbackserver.model;

public enum VolunteerType {

	ATTENDED("A"), UNREGISTERED("U"), ABSENT("N");

	private String code;

	private VolunteerType(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

}
