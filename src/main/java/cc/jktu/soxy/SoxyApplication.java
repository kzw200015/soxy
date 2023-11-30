package cc.jktu.soxy;

import cc.jktu.soxy.config.SoxyConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SoxyConfig.class)
public class SoxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoxyApplication.class, args);
    }

}
