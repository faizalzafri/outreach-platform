package org.outreach.outreachfeedbackserver.repo;

import java.util.ArrayList;
import java.util.List;

import org.outreach.outreachfeedbackserver.entity.EventSummaryEntity;
import org.outreach.outreachfeedbackserver.entity.IEventReport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EventReportsRepository extends CrudRepository<EventSummaryEntity, String> {

	@Query(value = "SELECT vea.event_name as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id group by vea.event_id", nativeQuery = true)
	List<IEventReport> findAllByEvents();

	@Query(value = "SELECT vea.base_location as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join	volunteer_feeback vf on vea.event_id = vf.event_id group by vea.base_location", nativeQuery = true)
	List<IEventReport> findAllByBaseLocation();

	@Query(value = "SELECT vea.beneficiary_name as name, avg(vf.score) as averageScore FROM vol_event_attended vea inner join volunteer_feeback vf on vea.event_id = vf.event_id group by vea.beneficiary_name;", nativeQuery = true)
	List<IEventReport> findAllByBenificiary();
	
	@Override
	default <S extends EventSummaryEntity> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele->{
			
			System.out.println(findById(ele.getEventId()));
			System.out.println(findById(ele.getEventId()).isPresent());
			
			if(!findById(ele.getEventId()).isPresent()) {
				save(ele);
			}
		});
		
		return null;
	}
	
}
