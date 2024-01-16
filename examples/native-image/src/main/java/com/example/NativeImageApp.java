package com.example;

import static org.springframework.aop.framework.AopProxyUtils.completeJdkProxyInterfaces;

import java.util.List;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@SpringBootApplication
// @EnableExchangeClients
//    @ImportRuntimeHints(Hint.class)
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

    @Bean
    PostApi postApi(RestClient.Builder builder) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(builder.build()))
                .build();
        return factory.createClient(PostApi.class);
    }

    //    @Bean
    //    static Processor httpExchangeBeanRegistrationAotProcessor() {
    //        return new Processor();
    //    }

    static class Hint implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            ProxyHints proxies = hints.proxies();
            proxies.registerJdkProxy(completeJdkProxyInterfaces(NativeImageApp.PostApi.class));
        }
    }
}
