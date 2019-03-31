package org.outreach.outreachfeedbackserver.controller;

import java.util.Objects;

import org.outreach.outreachfeedbackserver.entity.FeedbackScoreEntity;
import org.outreach.outreachfeedbackserver.model.FeedbackModel;
import org.outreach.outreachfeedbackserver.model.ResponseModel;
import org.outreach.outreachfeedbackserver.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/feedback")
public class FeedbackController {

	@Autowired
	private FeedbackService feedbackService;

	@PostMapping
	public ResponseModel saveFeedback(@RequestBody FeedbackModel fModel) {

		FeedbackScoreEntity savedEntity = feedbackService.save(fModel);
		ResponseModel reModel = new ResponseModel();

		if (Objects.isNull(savedEntity)) {
			reModel.setMessage("Feedback Already Submitted");
			reModel.setHttpStatus(HttpStatus.OK.value());
		} else {
			reModel.setMessage("Feedback Submitted Successfully");
			reModel.setHttpStatus(HttpStatus.OK.value());
		}
		return reModel;
	}
}
