package com.freemanan.starter.httpexchange.shaded;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link ShadedHttpServiceProxyFactory} tester.
 */
class ShadedHttpServiceProxyFactoryTest {

    /**
     * Because the code uses reflection to obtain the attribute values of {@link HttpServiceProxyFactory.Builder},
     * when upgrading the Spring Boot version, this test is necessary for discovering the property changes of {@link HttpServiceProxyFactory.Builder}.
     *
     * @see ShadedHttpServiceProxyFactory.Builder#Builder(HttpServiceProxyFactory.Builder)
     */
    @Test
    void testHttpServiceProxyFactoryBuilderProperties() {
        Class<HttpServiceProxyFactory.Builder> clz = HttpServiceProxyFactory.Builder.class;
        Field[] fields = clz.getDeclaredFields();

        assertThat(fields).hasSize(6);
        assertThat(Arrays.stream(fields).map(Field::getName))
                .containsExactlyInAnyOrder(
                        "clientAdapter",
                        "customArgumentResolvers",
                        "conversionService",
                        "embeddedValueResolver",
                        "reactiveAdapterRegistry",
                        "blockTimeout");
    }
}
