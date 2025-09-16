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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.ReactiveHttpRequestValues;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implements the invocation of an {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping}-annotated,
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpExchangeAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
final class ShadedHttpServiceMethod {

    private static final boolean REACTOR_PRESENT =
            ClassUtils.isPresent("reactor.core.publisher.Mono", ShadedHttpServiceMethod.class.getClassLoader());

    private final Method method;

    private final MethodParameter[] parameters;

    private final List<HttpServiceArgumentResolver> argumentResolvers;

    private final HttpRequestValues.Processor requestValuesProcessor;

    private final HttpRequestValuesInitializer requestValuesInitializer;

    private final ResponseFunction responseFunction;

    ShadedHttpServiceMethod(
            Method method,
            Class<?> containingClass,
            List<HttpServiceArgumentResolver> argumentResolvers,
            HttpRequestValues.Processor valuesProcessor,
            HttpExchangeAdapter adapter,
            @Nullable StringValueResolver embeddedValueResolver) {

        this.method = method;
        this.parameters = initMethodParameters(method);
        this.argumentResolvers = argumentResolvers;
        this.requestValuesProcessor = valuesProcessor;

        boolean isReactorAdapter = (REACTOR_PRESENT && adapter instanceof ReactorHttpExchangeAdapter);

        this.requestValuesInitializer = HttpRequestValuesInitializer.create(
                method,
                containingClass,
                embeddedValueResolver,
                (isReactorAdapter ? ReactiveHttpRequestValues::builder : HttpRequestValues::builder));

        this.responseFunction = (isReactorAdapter
                ? ReactorExchangeResponseFunction.create((ReactorHttpExchangeAdapter) adapter, method)
                : ExchangeResponseFunction.create(adapter, method));
    }

    private static MethodParameter[] initMethodParameters(Method method) {
        int count = method.getParameterCount();
        if (count == 0) {
            return new MethodParameter[0];
        }
        if (KotlinDetector.isSuspendingFunction(method)) {
            count -= 1;
        }

        DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
        MethodParameter[] parameters = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            parameters[i] = new SynthesizingMethodParameter(method, i);
            parameters[i].initParameterNameDiscovery(nameDiscoverer);
        }
        return parameters;
    }

    public Method getMethod() {
        return this.method;
    }

    public @Nullable Object invoke(@Nullable Object[] arguments) {
        HttpRequestValues.Builder requestValues = this.requestValuesInitializer.initializeRequestValuesBuilder();
        applyArguments(requestValues, arguments);
        this.requestValuesProcessor.process(this.method, this.parameters, arguments, requestValues);
        return this.responseFunction.execute(requestValues.build());
    }

    private void applyArguments(HttpRequestValues.Builder requestValues, @Nullable Object[] arguments) {
        Assert.isTrue(arguments.length == this.parameters.length, "Method argument mismatch");
        for (int i = 0; i < arguments.length; i++) {
            Object value = arguments[i];
            boolean resolved = false;
            for (HttpServiceArgumentResolver resolver : this.argumentResolvers) {
                if (resolver.resolve(value, this.parameters[i], requestValues)) {
                    resolved = true;
                    break;
                }
            }
            int index = i;
            Assert.state(
                    resolved,
                    () -> "Could not resolve parameter [" + this.parameters[index].getParameterIndex() + "] in "
                            + this.parameters[index].getExecutable().toGenericString() + ": No suitable resolver");
        }
    }

    /**
     * Factory for {@link HttpRequestValues} with values extracted from the type
     * and method-level {@link org.springframework.web.bind.annotation.RequestMapping @HttpRequest} annotations.
     */
    private record HttpRequestValuesInitializer(
            @Nullable HttpMethod httpMethod,
            @Nullable String url,
            @Nullable MediaType contentType,
            @Nullable List<MediaType> acceptMediaTypes,
            MultiValueMap<String, String> headers,
            @Nullable String version,
            Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

        public HttpRequestValues.Builder initializeRequestValuesBuilder() {
            HttpRequestValues.Builder requestValues = this.requestValuesSupplier.get();
            if (this.httpMethod != null) {
                requestValues.setHttpMethod(this.httpMethod);
            }
            if (this.url != null) {
                requestValues.setUriTemplate(this.url);
            }
            if (this.contentType != null) {
                requestValues.setContentType(this.contentType);
            }
            if (this.acceptMediaTypes != null) {
                requestValues.setAccept(this.acceptMediaTypes);
            }
            this.headers.forEach((name, values) -> values.forEach(value -> requestValues.addHeader(name, value)));
            if (this.version != null) {
                requestValues.setApiVersion(this.version);
            }
            return requestValues;
        }

        /**
         * Introspect the method and create the request factory for it.
         */
        public static HttpRequestValuesInitializer create(
                Method method,
                Class<?> containingClass,
                @Nullable StringValueResolver embeddedValueResolver,
                Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

            List<HttpRequestValuesInitializer.AnnotationDescriptor> methodHttpExchanges =
                    getAnnotationDescriptors(method);
            Assert.state(!methodHttpExchanges.isEmpty(), () -> "Expected @HttpExchange annotation on method " + method);
            Assert.state(
                    methodHttpExchanges.size() == 1,
                    () -> "Multiple @HttpExchange annotations found on method %s, but only one is allowed: %s"
                            .formatted(method, methodHttpExchanges));

            List<HttpRequestValuesInitializer.AnnotationDescriptor> typeHttpExchanges =
                    getAnnotationDescriptors(containingClass);
            Assert.state(
                    typeHttpExchanges.size() <= 1,
                    () -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
                            .formatted(containingClass, typeHttpExchanges));

            RequestMapping methodAnnotation =
                    AnnotatedElementUtils.findMergedAnnotation(containingClass, RequestMapping.class);
            RequestMapping typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

            HttpMethod httpMethod = initHttpMethod(typeAnnotation, methodAnnotation);
            String url = initUrl(typeAnnotation, methodAnnotation, embeddedValueResolver);
            MediaType contentType = initContentType(typeAnnotation, methodAnnotation);
            List<MediaType> acceptableMediaTypes = initAccept(typeAnnotation, methodAnnotation);
            MultiValueMap<String, String> headers =
                    initHeaders(typeAnnotation, methodAnnotation, embeddedValueResolver);
            String version = initVersion(typeAnnotation, methodAnnotation);

            return new HttpRequestValuesInitializer(
                    httpMethod, url, contentType, acceptableMediaTypes, headers, version, requestValuesSupplier);
        }

        @Nullable
        private static HttpMethod initHttpMethod(@Nullable RequestMapping typeAnnot, RequestMapping annot) {

            String value1 = (typeAnnot != null ? typeAnnot.method().length : 0) > 0
                    ? typeAnnot.method()[0].asHttpMethod().name()
                    : null;
            String value2 = (annot.method().length > 0)
                    ? annot.method()[0].asHttpMethod().name()
                    : null;

            if (StringUtils.hasText(value2)) {
                return HttpMethod.valueOf(value2);
            }

            if (StringUtils.hasText(value1)) {
                return HttpMethod.valueOf(value1);
            }

            return null;
        }

        @Nullable
        private static String initUrl(
                @Nullable RequestMapping typeAnnot,
                RequestMapping annot,
                @Nullable StringValueResolver embeddedValueResolver) {

            String url1 = (typeAnnot != null ? typeAnnot.value().length : 0) > 0
                    ? typeAnnot.value()[0]
                    : null;
            String url2 = annot.value()[0];

            if (embeddedValueResolver != null) {
                url1 = (url1 != null ? embeddedValueResolver.resolveStringValue(url1) : null);
                url2 = embeddedValueResolver.resolveStringValue(url2);
            }

            boolean hasUrl1 = StringUtils.hasText(url1);
            boolean hasUrl2 = StringUtils.hasText(url2);

            if (hasUrl1 && hasUrl2) {
                return (url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "") + url2);
            }

            if (!hasUrl1 && !hasUrl2) {
                return null;
            }

            return (hasUrl2 ? url2 : url1);
        }

        @Nullable
        private static MediaType initContentType(@Nullable RequestMapping typeAnnot, RequestMapping annot) {

            String value1 = (typeAnnot != null ? typeAnnot.consumes().length : 0) > 0
                    ? typeAnnot.consumes()[0]
                    : "";
            String value2 = (annot.consumes().length > 0) ? annot.consumes()[0] : "";

            if (StringUtils.hasText(value2)) {
                return MediaType.parseMediaType(value2);
            }

            if (StringUtils.hasText(value1)) {
                return MediaType.parseMediaType(value1);
            }

            return null;
        }

        @Nullable
        private static List<MediaType> initAccept(@Nullable RequestMapping typeAnnot, RequestMapping annot) {

            String[] value1 = (typeAnnot != null ? typeAnnot.produces() : null);
            String[] value2 = annot.produces();

            if (!ObjectUtils.isEmpty(value2)) {
                return MediaType.parseMediaTypes(Arrays.asList(value2));
            }

            if (!ObjectUtils.isEmpty(value1)) {
                return MediaType.parseMediaTypes(Arrays.asList(value1));
            }

            return null;
        }

        private static MultiValueMap<String, String> initHeaders(
                @Nullable RequestMapping typeAnnotation,
                RequestMapping methodAnnotation,
                @Nullable StringValueResolver embeddedValueResolver) {

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            if (typeAnnotation != null) {
                addHeaders(typeAnnotation.headers(), embeddedValueResolver, headers);
            }
            addHeaders(methodAnnotation.headers(), embeddedValueResolver, headers);
            return headers;
        }

        private static @Nullable String initVersion(
                @Nullable RequestMapping typeAnnotation, RequestMapping methodAnnotation) {

            if (StringUtils.hasText(methodAnnotation.version())) {
                return methodAnnotation.version();
            }
            if (typeAnnotation != null && StringUtils.hasText(typeAnnotation.version())) {
                return typeAnnotation.version();
            }
            return null;
        }

        private static void addHeaders(
                String[] rawValues,
                @Nullable StringValueResolver embeddedValueResolver,
                MultiValueMap<String, String> outputHeaders) {

            for (String rawValue : rawValues) {
                String[] pair = StringUtils.split(rawValue, "=");
                if (pair == null) {
                    continue;
                }
                String name = pair[0].trim();
                List<String> values = new ArrayList<>();
                for (String value : StringUtils.commaDelimitedListToSet(pair[1])) {
                    if (embeddedValueResolver != null) {
                        value = embeddedValueResolver.resolveStringValue(value);
                    }
                    if (value != null) {
                        value = value.trim();
                        values.add(value);
                    }
                }
                if (!values.isEmpty()) {
                    outputHeaders.addAll(name, values);
                }
            }
        }

        private static List<HttpRequestValuesInitializer.AnnotationDescriptor> getAnnotationDescriptors(
                AnnotatedElement element) {
            return MergedAnnotations.from(
                            element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
                    .stream(RequestMapping.class)
                    .filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
                    .map(HttpRequestValuesInitializer.AnnotationDescriptor::new)
                    .distinct()
                    .toList();
        }

        private static class AnnotationDescriptor {

            private final RequestMapping httpExchange;
            private final MergedAnnotation<?> root;

            @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
            AnnotationDescriptor(MergedAnnotation<RequestMapping> mergedAnnotation) {
                this.httpExchange = mergedAnnotation.synthesize();
                this.root = mergedAnnotation.getRoot();
            }

            @Override
            public boolean equals(Object obj) {
                return (obj instanceof HttpRequestValuesInitializer.AnnotationDescriptor that
                        && this.httpExchange.equals(that.httpExchange));
            }

            @Override
            public int hashCode() {
                return this.httpExchange.hashCode();
            }

            @Override
            public String toString() {
                return this.root.synthesize().toString();
            }
        }
    }

    /**
     * Execute a request, obtain a response, and adapt to the expected return type.
     */
    private interface ResponseFunction {

        @Nullable
        Object execute(HttpRequestValues requestValues);
    }

    private record ExchangeResponseFunction(Function<HttpRequestValues, @Nullable Object> responseFunction)
            implements ResponseFunction {

        @Override
        public @Nullable Object execute(HttpRequestValues requestValues) {
            return this.responseFunction.apply(requestValues);
        }

        /**
         * Create the {@code ResponseFunction} that matches the method return type.
         */
        public static ResponseFunction create(HttpExchangeAdapter client, Method method) {
            if (KotlinDetector.isSuspendingFunction(method)) {
                throw new IllegalStateException("Kotlin Coroutines are only supported with reactive implementations");
            }

            MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
            Class<?> paramType = param.getNestedParameterType();

            Function<HttpRequestValues, @Nullable Object> responseFunction;
            if (ClassUtils.isVoidType(paramType)) {
                responseFunction = requestValues -> {
                    client.exchange(requestValues);
                    return null;
                };
            } else if (paramType.equals(HttpHeaders.class)) {
                responseFunction = request -> asOptionalIfNecessary(client.exchangeForHeaders(request), param);
            } else if (paramType.equals(ResponseEntity.class)) {
                MethodParameter bodyParam = param.nested();
                if (bodyParam.getNestedParameterType().equals(Void.class)) {
                    responseFunction =
                            request -> asOptionalIfNecessary(client.exchangeForBodilessEntity(request), param);
                } else {
                    ParameterizedTypeReference<?> bodyTypeRef =
                            ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
                    responseFunction =
                            request -> asOptionalIfNecessary(client.exchangeForEntity(request, bodyTypeRef), param);
                }
            } else {
                ParameterizedTypeReference<?> bodyTypeRef =
                        ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
                responseFunction =
                        request -> asOptionalIfNecessary(client.exchangeForBody(request, bodyTypeRef), param);
            }

            return new ExchangeResponseFunction(responseFunction);
        }

        private static @Nullable Object asOptionalIfNecessary(@Nullable Object response, MethodParameter param) {
            return param.getParameterType().equals(Optional.class) ? Optional.ofNullable(response) : response;
        }
    }

    /**
     * {@link ResponseFunction} for {@link ReactorHttpExchangeAdapter}.
     */
    private record ReactorExchangeResponseFunction(
            Function<HttpRequestValues, Publisher<?>> responseFunction,
            @Nullable ReactiveAdapter returnTypeAdapter,
            boolean blockForOptional,
            @Nullable Duration blockTimeout)
            implements ResponseFunction {

        @Nullable
        public Object execute(HttpRequestValues requestValues) {

            Publisher<?> responsePublisher = this.responseFunction.apply(requestValues);

            if (this.returnTypeAdapter != null) {
                return this.returnTypeAdapter.fromPublisher(responsePublisher);
            }

            if (this.blockForOptional) {
                return (this.blockTimeout != null
                        ? ((Mono<?>) responsePublisher).blockOptional(this.blockTimeout)
                        : ((Mono<?>) responsePublisher).blockOptional());
            } else {
                return (this.blockTimeout != null
                        ? ((Mono<?>) responsePublisher).block(this.blockTimeout)
                        : ((Mono<?>) responsePublisher).block());
            }
        }

        /**
         * Create the {@code ResponseFunction} that matches the method return type.
         */
        public static ResponseFunction create(ReactorHttpExchangeAdapter client, Method method) {
            MethodParameter returnParam = new MethodParameter(method, -1);
            Class<?> returnType = returnParam.getParameterType();
            boolean isSuspending = KotlinDetector.isSuspendingFunction(method);
            if (isSuspending) {
                returnType = Mono.class;
            }

            ReactiveAdapter reactiveAdapter =
                    client.getReactiveAdapterRegistry().getAdapter(returnType);

            MethodParameter actualParam =
                    (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
            Class<?> actualType = isSuspending ? actualParam.getParameterType() : actualParam.getNestedParameterType();

            Function<HttpRequestValues, Publisher<?>> responseFunction;
            if (ClassUtils.isVoidType(actualType)) {
                responseFunction = client::exchangeForMono;
            } else if (reactiveAdapter != null && reactiveAdapter.isNoValue()) {
                responseFunction = client::exchangeForMono;
            } else if (actualType.equals(HttpHeaders.class)) {
                responseFunction = client::exchangeForHeadersMono;
            } else if (actualType.equals(ResponseEntity.class)) {
                MethodParameter bodyParam = isSuspending ? actualParam : actualParam.nested();
                Class<?> bodyType = bodyParam.getNestedParameterType();
                if (bodyType.equals(Void.class)) {
                    responseFunction = client::exchangeForBodilessEntityMono;
                } else {
                    ReactiveAdapter bodyAdapter =
                            client.getReactiveAdapterRegistry().getAdapter(bodyType);
                    responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter, isSuspending);
                }
            } else {
                responseFunction = initBodyFunction(client, actualParam, reactiveAdapter, isSuspending);
            }

            return new ReactorExchangeResponseFunction(
                    responseFunction, reactiveAdapter, returnType.equals(Optional.class), client.getBlockTimeout());
        }

        @SuppressWarnings("ConstantConditions")
        private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(
                ReactorHttpExchangeAdapter client,
                MethodParameter methodParam,
                @Nullable ReactiveAdapter reactiveAdapter,
                boolean isSuspending) {

            if (reactiveAdapter == null) {
                return request -> client.exchangeForEntityMono(
                        request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
            }

            Assert.isTrue(
                    reactiveAdapter.isMultiValue(),
                    "ResponseEntity body must be a concrete value or a multi-value Publisher");

            ParameterizedTypeReference<?> bodyType = ParameterizedTypeReference.forType(
                    isSuspending
                            ? methodParam.nested().getGenericParameterType()
                            : methodParam.nested().getNestedGenericParameterType());

            // Shortcut for Flux
            if (reactiveAdapter.getReactiveType().equals(Flux.class)) {
                return request -> client.exchangeForEntityFlux(request, bodyType);
            }

            return request -> client.exchangeForEntityFlux(request, bodyType).map(entity -> {
                Flux<?> entityBody = entity.getBody();
                Assert.state(entityBody != null, "Entity body must not be null");
                Object body = reactiveAdapter.fromPublisher(entityBody);
                return new ResponseEntity<>(body, entity.getHeaders(), entity.getStatusCode());
            });
        }

        private static Function<HttpRequestValues, Publisher<?>> initBodyFunction(
                ReactorHttpExchangeAdapter client,
                MethodParameter methodParam,
                @Nullable ReactiveAdapter reactiveAdapter,
                boolean isSuspending) {

            ParameterizedTypeReference<?> bodyType = ParameterizedTypeReference.forType(
                    isSuspending ? methodParam.getGenericParameterType() : methodParam.getNestedGenericParameterType());

            return (reactiveAdapter != null && reactiveAdapter.isMultiValue()
                    ? request -> client.exchangeForBodyFlux(request, bodyType)
                    : request -> client.exchangeForBodyMono(request, bodyType));
        }
    }
}
