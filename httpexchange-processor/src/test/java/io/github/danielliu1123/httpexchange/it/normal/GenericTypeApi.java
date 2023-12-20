package io.github.danielliu1123.httpexchange.it.normal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("/GenericTypeApi")
public interface GenericTypeApi<T> {

    @GetExchange
    String get();

    interface InnerInterfaceInGenericTypeApi {
        @GetExchange
        String get();
    }
}
