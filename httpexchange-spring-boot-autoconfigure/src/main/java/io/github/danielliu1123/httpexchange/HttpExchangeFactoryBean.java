package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * @author Freeman
 */
public class HttpExchangeFactoryBean implements SmartFactoryBean<Object>, BeanFactoryAware {

    private final Class<?> clientType;
    private final boolean isUseHttpExchangeAnnotation;

    private ConfigurableBeanFactory beanFactory;

    public HttpExchangeFactoryBean(Class<?> clientType, boolean isUseHttpExchangeAnnotation) {
        this.clientType = clientType;
        this.isUseHttpExchangeAnnotation = isUseHttpExchangeAnnotation;
    }

    @Nullable
    @Override
    public Object getObject() throws Exception {
        return new ExchangeClientCreator(beanFactory, clientType, isUseHttpExchangeAnnotation).create();
    }

    @Nullable
    @Override
    public Class<?> getObjectType() {
        return clientType;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public boolean isEagerInit() {
        return true;
    }
}
