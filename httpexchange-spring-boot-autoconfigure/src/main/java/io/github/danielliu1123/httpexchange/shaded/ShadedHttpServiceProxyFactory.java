/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.danielliu1123.httpexchange.shaded;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactor.MonoKt;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.AbstractReactorHttpExchangeAdapter;
import org.springframework.web.service.invoker.CookieValueArgumentResolver;
import org.springframework.web.service.invoker.HttpClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpMethodArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.PathVariableArgumentResolver;
import org.springframework.web.service.invoker.RequestAttributeArgumentResolver;
import org.springframework.web.service.invoker.RequestBodyArgumentResolver;
import org.springframework.web.service.invoker.RequestHeaderArgumentResolver;
import org.springframework.web.service.invoker.RequestParamArgumentResolver;
import org.springframework.web.service.invoker.RequestPartArgumentResolver;
import org.springframework.web.service.invoker.UriBuilderFactoryArgumentResolver;
import org.springframework.web.service.invoker.UrlArgumentResolver;
import reactor.core.publisher.Mono;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @see org.springframework.web.client.support.RestClientAdapter
 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
 * @see org.springframework.web.client.support.RestTemplateAdapter
 * @since 6.0
 */
public final class ShadedHttpServiceProxyFactory {

    private final HttpExchangeAdapter exchangeAdapter;

    private final List<HttpServiceArgumentResolver> argumentResolvers;

    @Nullable
    private final StringValueResolver embeddedValueResolver;

    private ShadedHttpServiceProxyFactory(
            HttpExchangeAdapter exchangeAdapter,
            List<HttpServiceArgumentResolver> argumentResolvers,
            @Nullable StringValueResolver embeddedValueResolver) {

        this.exchangeAdapter = exchangeAdapter;
        this.argumentResolvers = argumentResolvers;
        this.embeddedValueResolver = embeddedValueResolver;
    }

    /**
     * Return a proxy that implements the given HTTP service interface to perform
     * HTTP requests and retrieve responses through an HTTP client.
     *
     * @param serviceType the HTTP service to create a proxy for
     * @param <S>         the HTTP service type
     * @return the created proxy
     */
    public <S> S createClient(Class<S> serviceType) {

        List<ShadedHttpServiceMethod> httpServiceMethods =
                MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
                        .map(method -> createHttpServiceMethod(serviceType, method))
                        .toList();

        return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
    }

    private boolean isExchangeMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class)
                || AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private <S> ShadedHttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
        Assert.notNull(this.argumentResolvers, "No argument resolvers: afterPropertiesSet was not called");

        return new ShadedHttpServiceMethod(
                method, serviceType, this.argumentResolvers, this.exchangeAdapter, this.embeddedValueResolver);
    }

    /**
     * Return a builder that's initialized with the given client.
     *
     * @since 6.1
     */
    public static Builder builderFor(HttpExchangeAdapter exchangeAdapter) {
        return new Builder().exchangeAdapter(exchangeAdapter);
    }

    /**
     * Return a builder that's initialized with the given client.
     *
     * @deprecated in favor of {@link #builderFor(HttpExchangeAdapter)};
     * to be removed in 6.2.
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "6.1", forRemoval = true)
    public static Builder builder(HttpClientAdapter clientAdapter) {
        return new Builder().exchangeAdapter(clientAdapter.asReactorExchangeAdapter());
    }

    /**
     * Return an empty builder, with the client to be provided to builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an {@link ShadedHttpServiceProxyFactory}.
     */
    public static final class Builder {

        @Nullable
        private HttpExchangeAdapter exchangeAdapter;

        private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

        @Nullable
        private ConversionService conversionService;

        @Nullable
        private StringValueResolver embeddedValueResolver;

        private Builder() {}

        /**
         * Provide the HTTP client to perform requests through.
         *
         * @param adapter a client adapted to {@link HttpExchangeAdapter}
         * @return this same builder instance
         * @since 6.1
         */
        public Builder exchangeAdapter(HttpExchangeAdapter adapter) {
            this.exchangeAdapter = adapter;
            return this;
        }

        /**
         * Provide the HTTP client to perform requests through.
         *
         * @param clientAdapter a client adapted to {@link HttpClientAdapter}
         * @return this same builder instance
         * @deprecated in favor of {@link #exchangeAdapter(HttpExchangeAdapter)};
         * to be removed in 6.2
         */
        @SuppressWarnings("removal")
        @Deprecated(since = "6.1", forRemoval = true)
        public Builder clientAdapter(HttpClientAdapter clientAdapter) {
            this.exchangeAdapter = clientAdapter.asReactorExchangeAdapter();
            return this;
        }

        /**
         * Register a custom argument resolver, invoked ahead of default resolvers.
         *
         * @param resolver the resolver to add
         * @return this same builder instance
         */
        public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
            this.customArgumentResolvers.add(resolver);
            return this;
        }

        /**
         * Set the {@link ConversionService} to use where input values need to
         * be formatted as Strings.
         * <p>By default this is {@link DefaultFormattingConversionService}.
         *
         * @return this same builder instance
         */
        public Builder conversionService(ConversionService conversionService) {
            this.conversionService = conversionService;
            return this;
        }

        /**
         * Set the {@link StringValueResolver} to use for resolving placeholders
         * and expressions embedded in {@link HttpExchange#url()}.
         *
         * @param embeddedValueResolver the resolver to use
         * @return this same builder instance
         */
        public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
            this.embeddedValueResolver = embeddedValueResolver;
            return this;
        }

        /**
         * Set the {@link ReactiveAdapterRegistry} to use to support different
         * asynchronous types for HTTP service method return values.
         * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
         *
         * @return this same builder instance
         * @deprecated in favor of setting the same directly on the {@link HttpExchangeAdapter}
         */
        @Deprecated(since = "6.1", forRemoval = true)
        public Builder reactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
            if (this.exchangeAdapter instanceof AbstractReactorHttpExchangeAdapter settable) {
                settable.setReactiveAdapterRegistry(registry);
            }
            return this;
        }

        /**
         * Configure how long to block for the response of an HTTP service method
         * with a synchronous (blocking) method signature.
         * <p>By default this is not set, in which case the behavior depends on
         * connection and request timeout settings of the underlying HTTP client.
         * We recommend configuring timeout values directly on the underlying HTTP
         * client, which provides more control over such settings.
         *
         * @param blockTimeout the timeout value
         * @return this same builder instance
         * @deprecated in favor of setting the same directly on the {@link HttpExchangeAdapter}
         */
        @Deprecated(since = "6.1", forRemoval = true)
        public Builder blockTimeout(@Nullable Duration blockTimeout) {
            if (this.exchangeAdapter instanceof AbstractReactorHttpExchangeAdapter settable) {
                settable.setBlockTimeout(blockTimeout);
            }
            return this;
        }

        /**
         * Build the {@link ShadedHttpServiceProxyFactory} instance.
         */
        public ShadedHttpServiceProxyFactory build() {
            Assert.notNull(this.exchangeAdapter, "HttpClientAdapter is required");

            return new ShadedHttpServiceProxyFactory(
                    this.exchangeAdapter, initArgumentResolvers(), this.embeddedValueResolver);
        }

        @SuppressWarnings("DataFlowIssue")
        private List<HttpServiceArgumentResolver> initArgumentResolvers() {

            // Custom
            List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(this.customArgumentResolvers);

            ConversionService service = (this.conversionService != null
                    ? this.conversionService
                    : new DefaultFormattingConversionService());

            // Annotation-based
            resolvers.add(new RequestHeaderArgumentResolver(service));
            resolvers.add(new RequestBodyArgumentResolver(this.exchangeAdapter));
            resolvers.add(new PathVariableArgumentResolver(service));
            resolvers.add(new RequestParamArgumentResolver(service));
            resolvers.add(new RequestPartArgumentResolver(this.exchangeAdapter));
            resolvers.add(new CookieValueArgumentResolver(service));
            if (this.exchangeAdapter.supportsRequestAttributes()) {
                resolvers.add(new RequestAttributeArgumentResolver());
            }

            // Specific type
            resolvers.add(new UrlArgumentResolver());
            resolvers.add(new UriBuilderFactoryArgumentResolver());
            resolvers.add(new HttpMethodArgumentResolver());

            return resolvers;
        }
    }

    /**
     * {@link MethodInterceptor} that invokes an {@link ShadedHttpServiceMethod}.
     */
    private static final class HttpServiceMethodInterceptor implements MethodInterceptor {

        private final Map<Method, ShadedHttpServiceMethod> httpServiceMethods;

        private HttpServiceMethodInterceptor(List<ShadedHttpServiceMethod> methods) {
            this.httpServiceMethods =
                    methods.stream().collect(Collectors.toMap(ShadedHttpServiceMethod::getMethod, Function.identity()));
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            ShadedHttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
            if (httpServiceMethod != null) {
                if (KotlinDetector.isSuspendingFunction(method)) {
                    return KotlinDelegate.invokeSuspendingFunction(invocation, httpServiceMethod);
                }
                return httpServiceMethod.invoke(invocation.getArguments());
            }
            if (method.isDefault()) {
                if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
                    Object proxy = reflectiveMethodInvocation.getProxy();
                    return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
                }
            }
            throw new IllegalStateException("Unexpected method invocation: " + method);
        }
    }

    /**
     * Inner class to avoid a hard dependency on Kotlin at runtime.
     */
    @SuppressWarnings("unchecked")
    private static class KotlinDelegate {

        public static Object invokeSuspendingFunction(
                MethodInvocation invocation, ShadedHttpServiceMethod httpServiceMethod) {
            Object[] rawArguments = invocation.getArguments();
            Object[] arguments = resolveArguments(rawArguments);
            Continuation<Object> continuation = (Continuation<Object>) rawArguments[rawArguments.length - 1];
            Mono<Object> wrapped = (Mono<Object>) httpServiceMethod.invoke(arguments);
            assert wrapped != null;
            return MonoKt.awaitSingleOrNull(wrapped, continuation);
        }

        private static Object[] resolveArguments(Object[] args) {
            Object[] functionArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, functionArgs, 0, args.length - 1);
            return functionArgs;
        }
    }
}
