package com.freemanan.starter.httpexchange;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpClientsProperties.class)
class HttpClientsConfiguration implements DisposableBean {

    @Override
    public void destroy() {
        Cache.clear();
    }
}
