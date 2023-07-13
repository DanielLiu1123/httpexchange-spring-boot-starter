/*
 * Copyright 2013-2022 the original author or authors.
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

package com.freemanan.starter.httpexchange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Convert a Java bean to query parameters.
 *
 * <p> Correct usage:
 * <pre>{@code
 * @GetExchange
 * User get(@BeanParam User user);
 *
 * @GetExchange
 * User get(@RequestParam Map<String, Object> user);
 * }</pre>
 *
 * <p> NOTE: if you consider using {@link Map} as parameter type, you should use {@link RequestParam} instead.
 * <p> Incorrect usage:
 * <pre>{@code
 * @GetExchange
 * User get(@BeanParam Map<String, Object> user); // use @RequestParam instead
 * }</pre>
 *
 * @author Freeman
 * @since 3.1.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BeanParam {}
