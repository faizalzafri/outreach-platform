package org.outreach.outreachfeedbackserver.model;

public class EventReportModel {

	private String name;
	private Double averageScore;

	public EventReportModel(String name, Double averageScore) {
		super();
		this.name = name;
		this.averageScore = averageScore;
	}

	public EventReportModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getAverageScore() {
		return averageScore;
	}

	public void setAverageScore(Double averageScore) {
		this.averageScore = averageScore;
	}

}
