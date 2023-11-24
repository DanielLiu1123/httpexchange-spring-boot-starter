package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
                        "exchangeAdapter", "customArgumentResolvers", "conversionService", "embeddedValueResolver");
    }
}
