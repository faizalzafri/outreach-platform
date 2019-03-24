package org.outreach.outreachfeedbackserver.repo;

import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.UserRole;
import org.springframework.data.repository.CrudRepository;

public interface UserRoleRepository extends CrudRepository<UserRole, String> {

	public Optional<UserRole> findByAssociateId(String id);

	@Override
	default <S extends UserRole> Iterable<S> saveAll(Iterable<S> list) {
		list.forEach(ele -> {
			if (!findById(ele.getAssociateId()).isPresent()) {
				save(ele);
			}
		});
		return null;
	}
}
