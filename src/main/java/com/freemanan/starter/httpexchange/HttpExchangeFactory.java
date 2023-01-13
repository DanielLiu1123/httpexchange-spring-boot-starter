package com.freemanan.starter.httpexchange;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
public class HttpExchangeFactory {

    private final BeanFactory beanFactory;
    private final Class<?> type;

    public HttpExchangeFactory(BeanFactory beanFactory, Class<?> type) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(type, "type must not be null");
        this.beanFactory = beanFactory;
        this.type = type;
    }

    public Object create() {
        HttpServiceProxyFactory factory = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.class)
                .getIfUnique(() -> {
                    WebClient.Builder builder =
                            beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
                    return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(builder.build()))
                            .embeddedValueResolver(
                                    beanFactory.getBean(Environment.class)
                                            ::resolvePlaceholders) // support url placeholder '${}'
                            .build();
                });
        return factory.createClient(type);
    }
}
