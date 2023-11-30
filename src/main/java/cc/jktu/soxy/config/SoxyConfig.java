package cc.jktu.soxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "soxy")
public class SoxyConfig {

    private final Integer port;
    private final Map<String, ProxyConfig> proxyConfigs;

}
