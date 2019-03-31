package org.outreach.outreachfeedbackserver.service;

import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.FeedbackScoreEntity;
import org.outreach.outreachfeedbackserver.model.FeedbackModel;
import org.outreach.outreachfeedbackserver.repo.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

	@Autowired
	private FeedbackRepository fRepository;

	public FeedbackScoreEntity save(FeedbackModel fModel) {

		Optional<FeedbackScoreEntity> feedback = fRepository
				.findById(new EventPK(fModel.getEventId(), fModel.getEmployeeId()));

		boolean exists = feedback.isPresent();

		if (exists) {

			return null;
		}
		FeedbackScoreEntity fEntity = new FeedbackScoreEntity();
		fEntity.setEventPK(new EventPK(fModel.getEventId(), fModel.getEmployeeId()));
		fEntity.setScore(fModel.getScore());
		fEntity.setAnswer1(fModel.getAnswer1());
		fEntity.setAnswer2(fModel.getAnswer2());
		fEntity.setStatus("I");

		return fRepository.save(fEntity);

	}

}
