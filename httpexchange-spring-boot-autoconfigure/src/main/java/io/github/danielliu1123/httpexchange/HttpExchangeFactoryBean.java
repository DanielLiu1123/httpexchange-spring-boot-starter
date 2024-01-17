package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * @author Freeman
 * @since 3.2.2
 */
public class HttpExchangeFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

    private final Class<T> clientType;
    private final boolean isUseHttpExchangeAnnotation;

    private ConfigurableBeanFactory beanFactory;

    public HttpExchangeFactoryBean(Class<T> clientType, boolean isUseHttpExchangeAnnotation) {
        this.clientType = clientType;
        this.isUseHttpExchangeAnnotation = isUseHttpExchangeAnnotation;
    }

    @Nullable
    @Override
    public T getObject() throws Exception {
        return new ExchangeClientCreator(beanFactory, clientType, isUseHttpExchangeAnnotation).create();
    }

    @Nullable
    @Override
    public Class<T> getObjectType() {
        return clientType;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }
}
