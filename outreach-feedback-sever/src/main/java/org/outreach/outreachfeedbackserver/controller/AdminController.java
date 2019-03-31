package org.outreach.outreachfeedbackserver.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.outreach.outreachfeedbackserver.entity.IFeedbackStatus;
import org.outreach.outreachfeedbackserver.service.AdminManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class AdminController {

	@Autowired
	AdminManagementService adminService;

	@PostMapping(value = "/addAdmin")
	public String createAdmin(@RequestBody String id) {
		boolean alreadyExist = adminService.checkIfAlreadyExisted(id);
		if (!alreadyExist) {
			adminService.addAdmin(id);
			return id;
		}
		return "0";
	}

	@PostMapping(value = "/addPMO")
	public String createPmo(@RequestBody String id) {
		boolean alreadyExist = adminService.checkIfAlreadyExisted(id);
		if (!alreadyExist) {
			adminService.addPmo(id);
			return id;
		}
		return "0";
	}

	@GetMapping("/report")
	public List<IFeedbackStatus> getReport(HttpServletRequest request) {
		List<IFeedbackStatus> report = new ArrayList<>();
		report = adminService.getFeedbackStatusReport();
		return report;

	}

}
