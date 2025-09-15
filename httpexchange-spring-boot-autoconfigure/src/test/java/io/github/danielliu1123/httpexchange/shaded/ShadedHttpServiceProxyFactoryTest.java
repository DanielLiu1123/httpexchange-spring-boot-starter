package io.github.danielliu1123.httpexchange.shaded;

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
     * @see ShadedHttpServiceProxyFactory.Builder
     */
    @Test
    void testHttpServiceProxyFactoryBuilderProperties() {
        Class<HttpServiceProxyFactory.Builder> clz = HttpServiceProxyFactory.Builder.class;
        Field[] fields = clz.getDeclaredFields();

        assertThat(Arrays.stream(fields).map(Field::getName))
                .containsExactlyInAnyOrder(
                        "exchangeAdapter",
                        "customArgumentResolvers",
                        "conversionService",
                        "embeddedValueResolver",
                        "exchangeAdapterDecorator",
                        "requestValuesProcessors");
    }
}
