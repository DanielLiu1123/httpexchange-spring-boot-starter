package com.freemanan.starter.httpexchange;

import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Determine whether {@link WebClient} can be reused.
 *
 * @author Freeman
 */
record ReusableModel(String baseUrl, Long responseTimeout, Map<String, List<String>> headers) {}
