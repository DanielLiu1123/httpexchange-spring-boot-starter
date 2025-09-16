package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link HttpClientBeanDefinitionRegistry}
 */
class HttpClientBeanDefinitionRegistryTests {

    @AfterEach
    void clear() {
        HttpClientBeanDefinitionRegistry.scanInfo.clear();
    }

    /**
     * {@link HttpClientBeanDefinitionRegistry#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}
     */
    @Test
    void testPostProcessBeanDefinitionRegistry_disabled() {
        // Arrange
        var registry = buildHttpClientBeanDefinitionRegistry(Map.of(HttpExchangeProperties.PREFIX + ".enabled", false));
        var beanDefinitionRegistry = mock(BeanDefinitionRegistry.class);

        // Act
        registry.postProcessBeanDefinitionRegistry(beanDefinitionRegistry);

        // Assert
        verifyNoInteractions(beanDefinitionRegistry);
    }

    /**
     * {@link HttpClientBeanDefinitionRegistry#registerBeans(HttpClientBeanRegistrar)}
     */
    @Test
    void testRegisterBeans_withBasePackages() {
        // Arrange
        var registry = buildHttpClientBeanDefinitionRegistry(
                Map.of(HttpExchangeProperties.PREFIX + ".base-packages", "com.example,com.another"));

        var registrar = mock(HttpClientBeanRegistrar.class);
        var captor = ArgumentCaptor.forClass(String[].class);

        // Act
        registry.registerBeans(registrar);

        // Assert
        verify(registrar).register(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("com.example", "com.another");
        assertThat(HttpClientBeanDefinitionRegistry.scanInfo.basePackages)
                .containsExactlyInAnyOrder("com.example", "com.another");
    }

    private static HttpClientBeanDefinitionRegistry buildHttpClientBeanDefinitionRegistry(
            Map<String, Object> properties) {
        var env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("test", properties));
        return new HttpClientBeanDefinitionRegistry(env);
    }
}
