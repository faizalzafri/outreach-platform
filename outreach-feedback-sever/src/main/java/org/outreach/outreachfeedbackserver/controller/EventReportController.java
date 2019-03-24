package org.outreach.outreachfeedbackserver.controller;

import java.util.ArrayList;
import java.util.List;

import org.outreach.outreachfeedbackserver.entity.IEventReport;
import org.outreach.outreachfeedbackserver.service.EventSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@CrossOrigin(origins = "*")
public class EventReportController {

	@Autowired
	private EventSummaryService eventSummaryService;

	@RequestMapping("/{type}")
	public List<IEventReport> getReport(@PathVariable String type) {

		List<IEventReport> eventReport = new ArrayList<>();

		switch (type) {
		case "event":
			eventReport = eventSummaryService.getEventRatingByEvent();
			break;
		case "benificiary":
			eventReport = eventSummaryService.getEventRatingByBeneficiary();
			break;
		case "city":
			eventReport = eventSummaryService.getEventRatingByBaseLocation();
			break;

		}

		return eventReport;

	}

}
