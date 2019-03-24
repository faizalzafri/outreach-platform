package org.outreach.outreachfeedbackserver.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.outreach.outreachfeedbackserver.model.Roles;

@Entity
@Table(name = "user_role_access")
public class UserRole {

	@Id
	private String associateId;

	@Enumerated(EnumType.STRING)
	private Roles role;
	
	public UserRole() {
		
	}

	public UserRole(String associateId, Roles role) {
		super();
		this.associateId = associateId;
		this.role = role;
	}

	public String getAssociateId() {
		return associateId;
	}

	public void setAssociateId(String associateId) {
		this.associateId = associateId;
	}

	public Roles getRole() {
		return role;
	}

	public void setRole(Roles role) {
		this.role = role;
	}

}
