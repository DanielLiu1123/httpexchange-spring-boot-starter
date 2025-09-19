package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HttpExchangePropertiesTest {

    @Nested
    class MergeTests {

        @Test
        void shouldInheritDefaultValuesWhenChannelPropertiesAreNull() {
            // Arrange
            var properties = new HttpExchangeProperties();
            properties.setBaseUrl("http://default:8080");
            properties.setClientType(HttpExchangeProperties.ClientType.REST_CLIENT);
            properties.setLoadbalancerEnabled(false);
            properties.setHttpClientReuseEnabled(false);

            var channel = new HttpExchangeProperties.Channel();
            channel.setName("test-channel");
            // All channel properties are null, should inherit from defaults
            properties.setChannels(List.of(channel));

            // Act
            properties.merge();
            var actual = properties.getChannels().get(0);

            // Assert
            var expected = new HttpExchangeProperties.Channel();
            expected.setName("test-channel");
            expected.setBaseUrl("http://default:8080");
            expected.setClientType(HttpExchangeProperties.ClientType.REST_CLIENT);
            expected.setLoadbalancerEnabled(false);
            expected.setHttpClientReuseEnabled(false);
            expected.setHeaders(List.of());
            assertThat(actual).isEqualTo(expected);
        }
    }
}
