package org.outreach.outreachfeedbackserver;

import java.text.ParseException;

import org.outreach.outreachfeedbackserver.service.EmailService;
import org.outreach.outreachfeedbackserver.util.WatcherInputDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

@SpringBootApplication
@EnableEurekaClient
@EnableAsync
public class OutreachFeedbackApplication {

	@Autowired
	WatcherInputDirectory watcherInputDirectory;

	@Autowired
	private TaskExecutor taskExecutor;

	public static void main(String[] args) throws ParseException {
		SpringApplication.run(OutreachFeedbackApplication.class, args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		return new SimpleAsyncTaskExecutor(); // Or use another one of your liking
	}
	
	@Bean
	public static PasswordEncoder getPasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() throws ParseException {

		System.out.println("Monitor Start ::");

		taskExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					watcherInputDirectory.watch();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Bean
	@Primary
	public FreeMarkerConfigurationFactoryBean getFreeMarkerConfiguration() {
		FreeMarkerConfigurationFactoryBean bean = new FreeMarkerConfigurationFactoryBean();
		bean.setTemplateLoaderPath("classpath:/templates/");
		return bean;
	}

	/*@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

			System.out.println("Let's inspect the beans provided by Spring Boot:");

			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				System.out.println(beanName);
			}
		};
	}*/
	
	/*@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {

		Mail mail = new Mail();
		mail.setFrom("visueng16@gmail.com");
		mail.setTo("vishal.chauhan.mirage@gmail.com");
		mail.setSubject("TEST EMAIL PLEASE IGNORE");

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("name", "Vishal");
		model.put("location", "India");
		model.put("eventName","Bags of Joy");
		model.put("eventDate", "23 March 2019");
		model.put("feedbacklink", "http://localhost:4200/feedback/");
		model.put("signature", "OutReach Global");
		mail.setModel(model);

		emailService.sendSimpleMessage(mail,"");
	}*/
}
