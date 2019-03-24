package org.outreach.outreachfeedbackeurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class OutreachFeedbackEurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OutreachFeedbackEurekaServerApplication.class, args);
	}

}
