package org.outreach.outreachfeedbackauthserver.entities;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;


//@MappedSuperclass
public abstract class BaseEntity implements Serializable {
	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	// private Long id;
}