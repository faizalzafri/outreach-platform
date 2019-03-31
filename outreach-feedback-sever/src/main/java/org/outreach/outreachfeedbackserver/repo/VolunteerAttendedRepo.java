package org.outreach.outreachfeedbackserver.repo;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.VolunteerAttended;
import org.springframework.data.repository.CrudRepository;

public interface VolunteerAttendedRepo extends CrudRepository<VolunteerAttended, EventPK> {

	public Optional<VolunteerAttended> findByEventPK(EventPK eventPK);
	
	public List<VolunteerAttended> findByEmailStatus(String emailStatus);

	@Override
	default <S extends VolunteerAttended> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele -> {
			if (!findByEventPK(ele.getEventPK()).isPresent()) {
				save(ele);
			}
		});
		return null;
	}

}
