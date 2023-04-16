# Http Exchange Spring Boot Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/com.freemanan/httpexchange-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The missing starter for Spring 6.x declarative HTTP client. 

The goal is to provide a Spring Boot Starter for declarative HTTP client similar to `Spring Cloud OpenFeign`, but completely driven by configuration and without the need for any additional annotations.

## What is it

Spring 6.0 has provided its own support for declarative HTTP clients.

```java

@HttpExchange("https://my-json-server.typicode.com")
public interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}
```

This is basic usage:

```java

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args);

        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder builder) {
        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(builder.build()))
                .build();
    }

    @Bean
    PostApi postApi(HttpServiceProxyFactory factory) {
        return factory.createClient(UserClient.class);
    }

}

```

## Quick Start

```groovy
// gradle
implementation 'com.freemanan:httpexchange-spring-boot-starter:3.0.9'
```

```xml
<!-- maven -->
<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>httpexchange-spring-boot-starter</artifactId>
    <version>3.0.9</version>
</dependency>
```

You can simplify the code as follows:

```java

@SpringBootApplication
@EnableExchangeClients
public class App {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args);

        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }

}
```

If you have experiences with `Spring Cloud OpenFeign`, you will find that the usage is very similar.

`httpexhange-spring-boot-starter` will automatically scan the interfaces annotated with `@HttpExchange` and create the
corresponding beans.

## Core Features

- Automatically scan interfaces annotated with `@HttpExchange` and create corresponding beans.

  All you need to do is add the `@EnableExchangeClients` annotation to your main class.

- Support url variables.

  ```java
  @HttpExchange("${api.post.url}")
  public interface PostApi {
      @GetExchange("/typicode/demo/posts/{id}")
      Post getPost(@PathVariable("id") int id);
  }
  ```

- Support validation.

  ```java
  @HttpExchange("${api.post.url}")
  @Validated
  public interface PostApi {
      @GetExchange("/typicode/demo/posts/{id}")
      Post getPost(@PathVariable("id") @Min(1) @Max(3) int id);
  }
  ```
  NOTE: this feature needs `spring-boot` version >= 3.0.3,
  see [issue](https://github.com/spring-projects/spring-framework/issues/29782)
  and [tests](src/test/java/com/freemanan/starter/httpexchange/ValidationTests.java)

- Convert Java Bean to Query String.

  In Spring Web/WebFlux (server side), it will automatically convert query string to Java Bean,
  but `Spring Cloud OpenFeign` or `Exchange client of Spring 6` does not support to convert Java bean to query string by
  default. In `Spring Cloud OpenFeign` you need `@SpringQueryMap` to achieve this feature.

  `httpexhange-spring-boot-starter` supports this feature by default, and you don't need any another annotation.

  ```java
  public interface PostApi {
      @GetExchange
      List<Post> findAll(Post condition);
  }
  ```

  Auto convert non-null fields of `condition` to query string.

- Configuration Driven.

  `httpexhange-spring-boot-starter` provides a lot of configuration properties to customize the behavior of the client.

  You can configure the `base-url`, `timeout` and `headers` for each client, `httpexhange-spring-boot-starter` will
  reuse `WebClient` as much as possible.

  ```yaml
  http-exchange:
    base-url: http://api-gateway # global base-url
    response-timeout: 10000      # global timeout
    headers:                     # global headers
      - key: X-App-Name
        values: ${spring.application.name}
    clients:
      - name: OrderApi
        base-url: http://order   # client specific base-url, will override global base-url
        response-timeout: 1000   # client specific timeout, will override global timeout
        headers:                 # client specific headers, will merge with global headers
          - key: X-Key
            values: [value1, value2]
      - name: UserApi
        base-url: user
        response-timeout: 2000
      - client-class: com.example.FooApi
        base-url: service-foo.namespace
  ```

  `httpexhange-spring-boot-starter` use property `name` or `client-class` to identify the client, use `client-class`
  first if configured, otherwise use `name` to identify the client.

  For example, there is a client interface: `com.example.PostApi`, you can
  use `name: PostApi`, `name: com.example.PostApi`, `name: post-api` or `client-class: com.example.PostApi` to identify
  the client.

## Version

The major/minor version of this project is consistent with the version of `Spring Boot`. Therefore, `3.0.x` of this
project should work with `3.0.x` of `Spring Boot`. Please always use the latest version.

| Spring Boot | httpexchange-spring-boot-starter |
|-------------|----------------------------------|
| 3.0.x       | 3.0.9                            |

## License

The MIT License.
