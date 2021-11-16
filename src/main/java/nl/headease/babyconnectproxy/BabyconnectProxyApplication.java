package nl.headease.babyconnectproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class BabyconnectProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BabyconnectProxyApplication.class, args);
	}

}
