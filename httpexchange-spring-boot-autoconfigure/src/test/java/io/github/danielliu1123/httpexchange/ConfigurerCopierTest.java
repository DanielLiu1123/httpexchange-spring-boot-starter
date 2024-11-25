package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link ConfigurerCopier}
 */
class ConfigurerCopierTest {

    @Test
    void makeSureConfigurerPropertiesAreNotChanged() {

        assertThat(RestClientBuilderConfigurer.class)
                .hasOnlyDeclaredFields("requestFactoryBuilder", "requestFactorySettings", "customizers");

        assertThat(RestTemplateBuilderConfigurer.class)
                .hasOnlyDeclaredFields(
                        "requestFactoryBuilder",
                        "requestFactorySettings",
                        "httpMessageConverters",
                        "restTemplateCustomizers",
                        "restTemplateRequestCustomizers");
    }

    /**
     * {@link ConfigurerCopier#copyRestClientBuilderConfigurer(RestClientBuilderConfigurer)}
     */
    @Test
    void testCopyRestClientBuilderConfigurer() {

        var configurer = new RestClientBuilderConfigurer();

        ReflectionTestUtils.setField(configurer, "requestFactoryBuilder", ClientHttpRequestFactoryBuilder.jdk());
        ReflectionTestUtils.setField(configurer, "requestFactorySettings", ClientHttpRequestFactorySettings.defaults());
        ReflectionTestUtils.setField(configurer, "customizers", List.of());

        var result = ConfigurerCopier.copyRestClientBuilderConfigurer(configurer);

        assertThat(result).isNotSameAs(configurer);
        assertThat(ReflectionTestUtils.getField(result, "requestFactoryBuilder"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "requestFactoryBuilder"));
        assertThat(ReflectionTestUtils.getField(result, "requestFactorySettings"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "requestFactorySettings"));
        assertThat(ReflectionTestUtils.getField(result, "customizers"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "customizers"));
    }

    /**
     * {@link ConfigurerCopier#copyRestTemplateBuilderConfigurer(RestTemplateBuilderConfigurer)}
     */
    @Test
    void testCopyRestTemplateBuilderConfigurer() {

        var configurer = new RestTemplateBuilderConfigurer();

        ReflectionTestUtils.setField(configurer, "requestFactoryBuilder", ClientHttpRequestFactoryBuilder.jdk());
        ReflectionTestUtils.setField(configurer, "requestFactorySettings", ClientHttpRequestFactorySettings.defaults());
        ReflectionTestUtils.setField(configurer, "httpMessageConverters", null);
        ReflectionTestUtils.setField(configurer, "restTemplateCustomizers", List.of());
        ReflectionTestUtils.setField(configurer, "restTemplateRequestCustomizers", List.of());

        var result = ConfigurerCopier.copyRestTemplateBuilderConfigurer(configurer);

        assertThat(result).isNotSameAs(configurer);
        assertThat(ReflectionTestUtils.getField(result, "requestFactoryBuilder"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "requestFactoryBuilder"));
        assertThat(ReflectionTestUtils.getField(result, "requestFactorySettings"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "requestFactorySettings"));
        assertThat(ReflectionTestUtils.getField(result, "httpMessageConverters"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "httpMessageConverters"));
        assertThat(ReflectionTestUtils.getField(result, "restTemplateCustomizers"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "restTemplateCustomizers"));
        assertThat(ReflectionTestUtils.getField(result, "restTemplateRequestCustomizers"))
                .isSameAs(ReflectionTestUtils.getField(configurer, "restTemplateRequestCustomizers"));
    }

    /**
     * {@link ConfigurerCopier#setRestClientBuilderConfigurerProperty(RestClientBuilderConfigurer, String, Object)}
     */
    @Test
    void testSetRestClientBuilderConfigurerProperty() {
        var configurer = new RestClientBuilderConfigurer();

        var fb = ClientHttpRequestFactoryBuilder.jdk();

        ConfigurerCopier.setRestClientBuilderConfigurerProperty(configurer, "requestFactoryBuilder", fb);

        assertThat(ReflectionTestUtils.getField(configurer, "requestFactoryBuilder"))
                .isSameAs(fb);
    }

    /**
     * {@link ConfigurerCopier#setRestTemplateBuilderConfigurerProperty(RestTemplateBuilderConfigurer, String, Object)}
     */
    @Test
    void testSetRestTemplateBuilderConfigurerProperty() {
        var configurer = new RestTemplateBuilderConfigurer();

        var fb = ClientHttpRequestFactoryBuilder.jdk();

        ConfigurerCopier.setRestTemplateBuilderConfigurerProperty(configurer, "requestFactoryBuilder", fb);

        assertThat(ReflectionTestUtils.getField(configurer, "requestFactoryBuilder"))
                .isSameAs(fb);
    }
}
