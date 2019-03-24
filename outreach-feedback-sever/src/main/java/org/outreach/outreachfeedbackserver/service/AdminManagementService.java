package org.outreach.outreachfeedbackserver.service;

import java.util.List;
import java.util.Optional;

import org.outreach.outreachfeedbackserver.entity.UserRole;
import org.outreach.outreachfeedbackserver.model.Roles;
import org.outreach.outreachfeedbackserver.repo.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminManagementService {

	@Autowired
	private UserRoleRepository userRoleRepository;
	
	public void addAdmin(String associateId) {
		userRoleRepository.save(new UserRole(associateId,Roles.ADMIN));
	}
	
	public void addPmo(String associateId) {
		userRoleRepository.save(new UserRole(associateId,Roles.PMO));
	}
	
	public void addPmoMulitple(List<UserRole> associateIds) {
	
		userRoleRepository.saveAll(associateIds);
	}
	public boolean checkIfAlreadyExisted(String ids) {
		
		Optional<UserRole> userRole =userRoleRepository.findByAssociateId(ids);
		return userRole.isPresent();
	}
}
