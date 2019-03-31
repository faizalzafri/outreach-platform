package org.outreach.outreachfeedbackserver.repo;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.VolunteerNotAttended;
import org.springframework.data.repository.CrudRepository;

public interface VolunteerNotAttendedRepo extends CrudRepository<VolunteerNotAttended, EventPK> {
	public Optional<VolunteerNotAttended> findByEventPK(EventPK eventPK);

	@Override
	default <S extends VolunteerNotAttended> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele -> {
			if (!findByEventPK(ele.getEventPK()).isPresent()) {
				save(ele);
			}
		});
		return null;
	}

	public List<VolunteerNotAttended> findByEmailStatus(String string);

}
