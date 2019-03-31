package org.outreach.outreachfeedbackserver.service;

import java.util.List;

import org.outreach.outreachfeedbackserver.entity.IEventReport;
import org.outreach.outreachfeedbackserver.repo.EventReportsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventSummaryService {

	@Autowired
	private EventReportsRepository eRepository;

	public List<IEventReport> getEventRatingByEvent(String pocId) {

		if (pocId == null || "".equals(pocId)) {
			return eRepository.findAllByEvents();
		}
		return eRepository.findAllByPocEvents(pocId);
	}

	public List<IEventReport> getEventRatingByBaseLocation() {

		return eRepository.findAllByBaseLocation();
	}

	public List<IEventReport> getEventRatingByBeneficiary() {

		return eRepository.findAllByBenificiary();
	}

	void getEventRatingGlobal() {
	}
}
