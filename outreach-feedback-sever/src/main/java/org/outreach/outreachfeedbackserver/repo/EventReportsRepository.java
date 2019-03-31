package org.outreach.outreachfeedbackserver.repo;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.EventSummaryEntity;
import org.outreach.outreachfeedbackserver.entity.IEventReport;
import org.outreach.outreachfeedbackserver.entity.IFeedbackStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EventReportsRepository extends CrudRepository<EventSummaryEntity, Long> {

	@Query(value = "SELECT vea.event_name as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id group by vea.event_id", nativeQuery = true)
	List<IEventReport> findAllByEvents();

	@Query(value = "SELECT vea.event_name as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id and vf.event_id in (select event_id from event_summary where poc_id= ?1) group by vea.event_id;", nativeQuery = true)
	List<IEventReport> findAllByPocEvents(String pocId);

	@Query(value = "SELECT vea.base_location as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join	volunteer_feeback vf on vea.event_id = vf.event_id group by vea.base_location", nativeQuery = true)
	List<IEventReport> findAllByBaseLocation();

	@Query(value = "SELECT vea.beneficiary_name as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id group by vea.beneficiary_name", nativeQuery = true)
	List<IEventReport> findAllByBenificiary();

	@Query(value = "SELECT vf.event_id as eventId, vf.employee_id as employeeId, vea.email_status as emailStatus, vf.status as feedbackStatus FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id and vea.employee_id = vf.employee_id", nativeQuery = true)
	List<IFeedbackStatus> findAllByStatus();

	Optional<EventSummaryEntity> findByEventIdAndPocId(String eventId, String pocId);

	@Override
	default <S extends EventSummaryEntity> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele -> {

			if (!findByEventIdAndPocId(ele.getEventId(), ele.getPocId()).isPresent()) {
				save(ele);
			}
		});

		return null;
	}

}
