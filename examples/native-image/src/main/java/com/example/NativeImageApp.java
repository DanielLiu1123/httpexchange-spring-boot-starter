package com.example;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import io.github.danielliu1123.httpexchange.HttpExchangeFactoryBean;
import java.util.List;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@SpringBootApplication
@EnableExchangeClients
@ImportRuntimeHints(Hint.class)
public class NativeImageApp {

    public static void main(String[] args) {
        SpringApplication.run(NativeImageApp.class, args);
    }

    @HttpExchange("https://my-json-server.typicode.com")
    interface PostApi {
        record Post(Integer id, String title) {}

        @GetExchange("/typicode/demo/posts")
        List<Post> list();
    }

    @Bean
    ApplicationRunner runner(PostApi postApi) {
        return args -> postApi.list().forEach(System.out::println);
    }

    //    @Bean
    //    static Processor httpExchangeBeanRegistrationAotProcessor() {
    //        return new Processor();
    //    }
}

class Hint implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(NativeImageApp.PostApi.class));
    }
}

class Processor implements BeanRegistrationAotProcessor {

    @Nullable
    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        Class<?> beanClass = registeredBean.getBeanClass();
        if (!HttpExchangeFactoryBean.class.isAssignableFrom(beanClass)) {
            return null;
        }
        return null;
    }

    private static class AotContribution implements BeanRegistrationAotContribution {

        private final List<Class<?>> httpExchangeInterfaces;

        public AotContribution(List<Class<?>> httpExchangeInterfaces) {
            this.httpExchangeInterfaces = httpExchangeInterfaces;
        }

        @Override
        public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
            ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
            for (Class<?> httpExchangeInterface : this.httpExchangeInterfaces) {
                proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(httpExchangeInterface));
            }
        }
    }
}
