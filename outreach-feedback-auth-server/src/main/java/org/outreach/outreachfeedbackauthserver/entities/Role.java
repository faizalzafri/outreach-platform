package org.outreach.outreachfeedbackauthserver.entities;

import javax.persistence.Entity;

@Entity
public class Role extends BaseEntity {

	private String name;

	public Role() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Role(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
