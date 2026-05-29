package br.local.scanbridge;

import br.local.scanbridge.scanner.ScanProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScanProperties.class)
public class ScanBridgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScanBridgeApplication.class, args);
	}

}
