package org.outreach.outreachfeedbackserver.service;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.IFeedbackStatus;
import org.outreach.outreachfeedbackserver.entity.UserRole;
import org.outreach.outreachfeedbackserver.model.Roles;
import org.outreach.outreachfeedbackserver.repo.EventReportsRepository;
import org.outreach.outreachfeedbackserver.repo.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminManagementService {

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EventReportsRepository eRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	public void addAdmin(String associateId) {
		userRoleRepository.save(new UserRole(associateId + "@cognizant.com", // email
				associateId, // username
				passwordEncoder.encode(associateId), // password
				true, true, true, true, Roles.ROLE_ADMIN));
	}

	public void addPmo(String associateId) {
		userRoleRepository.save(new UserRole(associateId + "@cognizant.com", // email
				associateId, // username
				passwordEncoder.encode(associateId), // password
				true, true, true, true, Roles.ROLE_PMO));
	}

	public void addPmoMulitple(List<UserRole> associateIds) {

		userRoleRepository.saveAll(associateIds);
	}

	public boolean checkIfAlreadyExisted(String ids) {

		Optional<UserRole> userRole = userRoleRepository.findByAssociateId(ids);
		return userRole.isPresent();
	}

	public List<IFeedbackStatus> getFeedbackStatusReport() {

		return eRepository.findAllByStatus();

	}
}
