package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * {@link HttpExchangeRuntimeHintsRegistrar} tester.
 */
class HttpExchangeRuntimeHintsRegistrarTest {

    /**
     * {@link HttpExchangeRuntimeHintsRegistrar#listFactory()}
     */
    @Test
    void testListFactory() {
        HttpExchangeRuntimeHintsRegistrar registrar = new HttpExchangeRuntimeHintsRegistrar();
        Set<Class<?>> classes = registrar.listFactory();

        assertThat(classes)
                .contains(JdkClientHttpRequestFactory.class)
                .contains(AbstractClientHttpRequestFactoryWrapper.class)
                .contains(InterceptingClientHttpRequestFactory.class);
    }
}
