package io.bdrc.iiif.presentation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "io.bdrc.iiif" })

public class SpringBootIIIFPres extends SpringBootServletInitializer {

	public final static Logger log = LoggerFactory.getLogger(SpringBootIIIFPres.class.getName());

	public static void main(String[] args) throws Exception {

		try {
			InputStream input = SpringBootIIIFPres.class.getClassLoader().getResourceAsStream("iiifpres.properties");
			Properties props = new Properties();
			props.load(input);
			try {
				InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
				props.load(is);

			} catch (Exception ex) {
				// do nothing, continue props initialization
			}
			AuthProps.init(props);
			if ("true".equals(AuthProps.getProperty("useAuth"))) {
				RdfAuthModel.init();
			}
			ServiceCache.init();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("SpringBootIIIFPres has been properly initialized");
		SpringApplication.run(SpringBootIIIFPres.class, args);
	}

}
