package com.freemanan.starter.httpexchange.filter;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
public class TimeoutExchangeFilter implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(TimeoutExchangeFilter.class);

    private final long timeoutMs;

    /**
     * If the timeout is less than or equal to zero, the timeout will be disabled.
     *
     * @param timeoutMs timeout in milliseconds
     */
    public TimeoutExchangeFilter(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Mono<ClientResponse> resp = next.exchange(request);
        return timeoutMs > 0
                ? resp.timeout(Duration.ofMillis(timeoutMs))
                        .doOnError(
                                TimeoutException.class,
                                e -> log.warn("Request timeout after {} ms: {}", timeoutMs, request.url()))
                : resp;
    }
}
