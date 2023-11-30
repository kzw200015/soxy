package cc.jktu.soxy.config;


import lombok.Data;

@Data
public class ProxyConfig {

    private final Protocol protocol;
    private final String upstream;
    private final String socks;

}