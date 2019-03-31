package org.outreach.outreachfeedbackserver.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.outreach.outreachfeedbackserver.entity.EventInformationEntity;
import org.outreach.outreachfeedbackserver.entity.VolunteerAttended;
import org.outreach.outreachfeedbackserver.entity.VolunteerNotAttended;
import org.outreach.outreachfeedbackserver.entity.VolunteerUnregistered;
import org.outreach.outreachfeedbackserver.model.Mail;
import org.outreach.outreachfeedbackserver.repo.VolunteerAttendedRepo;
import org.outreach.outreachfeedbackserver.repo.VolunteerNotAttendedRepo;
import org.outreach.outreachfeedbackserver.repo.VolunteerUnregisteredRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender emailSender;

	@Autowired
	private Configuration freemarkerConfig;

	@Autowired
	private VolunteerAttendedRepo volunteerAttendedRepo;

	@Autowired
	private VolunteerNotAttendedRepo volunteerNotAttendedRepo;

	@Autowired
	private VolunteerUnregisteredRepo volunteerUnregisteredRepo;

	@Async
	public boolean sendSimpleMessage(Mail mail, String template) {
		try {
			MimeMessage message = emailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());

			helper.addAttachment("logo.png", new ClassPathResource("memorynotfound-logo.png"));

			Template t = freemarkerConfig.getTemplate("email-template.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(t, mail.getModel());

			helper.setTo(mail.getTo());
			helper.setText(html, true);
			helper.setSubject(mail.getSubject());
			helper.setFrom(mail.getFrom());

			emailSender.send(message);

			return true;
		} catch (MessagingException me) {
			System.out.println(me.getMessage());
			return false;
		} catch (IOException ie) {
			System.out.println(ie.getMessage());
			return false;
		} catch (TemplateException te) {
			System.out.println(te.getMessage());
			return false;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

	public void sendBatchEmails(List<? extends EventInformationEntity> list, String templateId) {

		list.stream().forEach(a -> {
			boolean success = false;
			Mail mail = new Mail();
			mail.setFrom("visueng16@gmail.com");
			mail.setTo("vishal.chauhan.mirage@gmail.com");
			mail.setSubject("Outreach Event Feedback Notification");
			Map<String, Object> model = mail.getModel();
			model.put("name", "Associate");
			model.put("location", a.getBaseLocation());
			model.put("eventName", a.getEventName());
			model.put("eventDate", a.getEventDate());
			model.put("feedbacklink", "http://localhost:4200/feedback/");
			model.put("signature", "OutReach Global");
			mail.setModel(model);

			success = sendSimpleMessage(mail, templateId);
			if (success) {
				a.setEmailStatus("P");
				if(a instanceof VolunteerAttended) {
					volunteerAttendedRepo.save((VolunteerAttended)a);
				}else if(a instanceof VolunteerNotAttended) {
					volunteerNotAttendedRepo.save((VolunteerNotAttended)a);
				}else if(a instanceof VolunteerUnregistered) {
					volunteerUnregisteredRepo.save((VolunteerUnregistered)a);
				}
			}

		});
	}

}
