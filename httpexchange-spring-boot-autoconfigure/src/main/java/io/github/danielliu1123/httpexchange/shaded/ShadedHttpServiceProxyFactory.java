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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.CookieValueArgumentResolver;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpMethodArgumentResolver;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.PathVariableArgumentResolver;
import org.springframework.web.service.invoker.RequestAttributeArgumentResolver;
import org.springframework.web.service.invoker.RequestBodyArgumentResolver;
import org.springframework.web.service.invoker.RequestHeaderArgumentResolver;
import org.springframework.web.service.invoker.RequestParamArgumentResolver;
import org.springframework.web.service.invoker.RequestPartArgumentResolver;
import org.springframework.web.service.invoker.UriBuilderFactoryArgumentResolver;
import org.springframework.web.service.invoker.UrlArgumentResolver;

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
 * @since 6.0
 */
public final class ShadedHttpServiceProxyFactory {

    private final HttpExchangeAdapter exchangeAdapter;

    private final List<HttpServiceArgumentResolver> argumentResolvers;

    private final HttpRequestValues.Processor requestValuesProcessor;

    private final @Nullable StringValueResolver embeddedValueResolver;

    private ShadedHttpServiceProxyFactory(
            HttpExchangeAdapter exchangeAdapter,
            List<HttpServiceArgumentResolver> argumentResolvers,
            List<HttpRequestValues.Processor> requestValuesProcessor,
            @Nullable StringValueResolver embeddedValueResolver) {

        this.exchangeAdapter = exchangeAdapter;
        this.argumentResolvers = argumentResolvers;
        this.requestValuesProcessor = new CompositeHttpRequestValuesProcessor(requestValuesProcessor);
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

        return getProxy(serviceType, httpServiceMethods);
    }

    @SuppressWarnings("unchecked")
    private <S> S getProxy(Class<S> serviceType, List<ShadedHttpServiceMethod> httpServiceMethods) {
        MethodInterceptor interceptor = new HttpServiceMethodInterceptor(httpServiceMethods);
        ProxyFactory factory = new ProxyFactory(serviceType, interceptor);
        return (S) factory.getProxy(serviceType.getClassLoader());
    }

    private boolean isExchangeMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class)
                || AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private <S> ShadedHttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
        Assert.notNull(this.argumentResolvers, "No argument resolvers: afterPropertiesSet was not called");

        return new ShadedHttpServiceMethod(
                method,
                serviceType,
                this.argumentResolvers,
                this.requestValuesProcessor,
                this.exchangeAdapter,
                this.embeddedValueResolver);
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

        private Function<HttpExchangeAdapter, HttpExchangeAdapter> exchangeAdapterDecorator = Function.identity();

        private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

        @Nullable
        private ConversionService conversionService;

        private final List<HttpRequestValues.Processor> requestValuesProcessors = new ArrayList<>();

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
         * Provide a function to wrap the configured {@code HttpExchangeAdapter}.
         * @param decorator a client adapted to {@link HttpExchangeAdapter}
         * @return this same builder instance
         * @since 7.0
         */
        public ShadedHttpServiceProxyFactory.Builder exchangeAdapterDecorator(
                Function<HttpExchangeAdapter, HttpExchangeAdapter> decorator) {
            this.exchangeAdapterDecorator = this.exchangeAdapterDecorator.andThen(decorator);
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
         * Register an {@link HttpRequestValues} processor that can further
         * customize request values based on the method and all arguments.
         * @param processor the processor to add
         * @return this same builder instance
         * @since 7.0
         */
        public ShadedHttpServiceProxyFactory.Builder httpRequestValuesProcessor(HttpRequestValues.Processor processor) {
            this.requestValuesProcessors.add(processor);
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
         * Build the {@link ShadedHttpServiceProxyFactory} instance.
         */
        /**
         * Build the {@link HttpServiceProxyFactory} instance.
         */
        public ShadedHttpServiceProxyFactory build() {
            Assert.notNull(this.exchangeAdapter, "HttpClientAdapter is required");
            HttpExchangeAdapter adapterToUse = this.exchangeAdapterDecorator.apply(this.exchangeAdapter);

            return new ShadedHttpServiceProxyFactory(
                    adapterToUse, initArgumentResolvers(), this.requestValuesProcessors, this.embeddedValueResolver);
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
                Object[] arguments = KotlinDetector.isSuspendingFunction(method)
                        ? resolveCoroutinesArguments(invocation.getArguments())
                        : invocation.getArguments();
                return httpServiceMethod.invoke(arguments);
            }
            if (method.isDefault()) {
                if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
                    Object proxy = reflectiveMethodInvocation.getProxy();
                    return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
                }
            }
            throw new IllegalStateException("Unexpected method invocation: " + method);
        }

        private static Object[] resolveCoroutinesArguments(@Nullable Object[] args) {
            if (args == null) {
                throw new IllegalStateException("Unexpected null arguments");
            }
            Object[] functionArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, functionArgs, 0, args.length - 1);
            return functionArgs;
        }
    }

    /**
     * Processor that delegates to a list of other processors.
     */
    private record CompositeHttpRequestValuesProcessor(List<HttpRequestValues.Processor> processors)
            implements HttpRequestValues.Processor {

        @Override
        public void process(
                Method method,
                MethodParameter[] parameters,
                @Nullable Object[] arguments,
                HttpRequestValues.Builder builder) {

            for (HttpRequestValues.Processor processor : this.processors) {
                processor.process(method, parameters, arguments, builder);
            }
        }
    }
}
