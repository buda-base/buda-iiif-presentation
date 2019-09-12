package io.bdrc.iiif.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "io.bdrc" })

public class SpringBootIIIFPres extends SpringBootServletInitializer {

	public final static Logger log = LoggerFactory.getLogger(SpringBootIIIFPres.class.getName());

	public static void main(String[] args) throws Exception {

		log.info("SpringBootIIIFPres has been properly initialized");
		SpringApplication.run(SpringBootIIIFPres.class, args);
	}

}
