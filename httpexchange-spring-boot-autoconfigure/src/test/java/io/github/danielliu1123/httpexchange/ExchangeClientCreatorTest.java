package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.ExchangeClientCreator.hasReactiveReturnTypeMethod;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeClientCreator} tester.
 */
class ExchangeClientCreatorTest {

    /**
     * {@link ExchangeClientCreator#shadedProxyFactory(HttpServiceProxyFactory.Builder)}
     */
    @Test
    void testShadedProxyFactory() {
        // used reflection, need to check whether fields are changed
        assertThat(HttpServiceProxyFactory.Builder.class)
                .hasOnlyDeclaredFields(
                        "exchangeAdapter",
                        "customArgumentResolvers",
                        "conversionService",
                        "embeddedValueResolver",
                        "exchangeAdapterDecorator",
                        "requestValuesProcessors");
    }

    /**
     * {@link ExchangeClientCreator#hasReactiveReturnTypeMethod(Class)}
     */
    @Test
    void testHasReactiveReturnTypeMethod() {

        interface AllReactiveReturnTypeNoAnnotationInterface {
            Mono<String> reactiveReturnTypeMethod1();

            Flux<String> reactiveReturnTypeMethod2();
        }

        interface AllReactiveReturnTypeInterface {
            @HttpExchange
            Mono<String> reactiveReturnTypeMethod1();

            @HttpExchange
            Flux<String> reactiveReturnTypeMethod2();
        }

        interface AllNormalReturnTypeInterface {
            @HttpExchange
            String normalReturnTypeMethod1();

            @HttpExchange
            String normalReturnTypeMethod2();
        }

        interface MixedReturnTypeInterface {
            @RequestMapping
            Mono<String> reactiveReturnTypeMethod1();

            @RequestMapping
            String normalReturnTypeMethod1();
        }

        assertThat(hasReactiveReturnTypeMethod(AllReactiveReturnTypeNoAnnotationInterface.class))
                .isFalse();
        assertThat(hasReactiveReturnTypeMethod(AllReactiveReturnTypeInterface.class))
                .isTrue();
        assertThat(hasReactiveReturnTypeMethod(AllNormalReturnTypeInterface.class))
                .isFalse();
        assertThat(hasReactiveReturnTypeMethod(MixedReturnTypeInterface.class)).isTrue();
    }
}
