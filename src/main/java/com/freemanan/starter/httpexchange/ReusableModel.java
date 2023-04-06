package com.freemanan.starter.httpexchange;

import java.util.List;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Determine whether {@link WebClient} can be reused.
 *
 * @author Freeman
 */
record ReusableModel(String baseUrl, Long responseTimeout, List<HttpClientsProperties.Header> headers) {}
