package org.outreach.outreachfeedbackserver.repo;

import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.FeedbackScoreEntity;
import org.springframework.data.repository.CrudRepository;

public interface FeedbackRepository extends CrudRepository<FeedbackScoreEntity, EventPK> {
	
	
}
