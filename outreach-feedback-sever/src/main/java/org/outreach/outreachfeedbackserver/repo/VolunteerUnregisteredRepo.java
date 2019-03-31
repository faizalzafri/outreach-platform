package org.outreach.outreachfeedbackserver.repo;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.VolunteerUnregistered;
import org.springframework.data.repository.CrudRepository;

public interface VolunteerUnregisteredRepo extends CrudRepository<VolunteerUnregistered, EventPK> {

	public Optional<VolunteerUnregistered> findByEventPK(EventPK eventPK);

	@Override
	default <S extends VolunteerUnregistered> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele -> {
			if (!findByEventPK(ele.getEventPK()).isPresent()) {
				save(ele);
			}
		});
		return null;
	}

	public List<VolunteerUnregistered> findByEmailStatus(String string);

}
