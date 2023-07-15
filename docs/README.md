## Introduction

`httpexchange-spring-boot-starter` is the missing starter for Spring 6.x declarative HTTP client.

Spring 6.x has provided its native support for declarative HTTP clients, you don't need to
use [Spring Cloud OpenFeign](https://github.com/spring-cloud/spring-cloud-openfeign)
or [Spring Cloud Square](https://github.com/spring-projects-experimental/spring-cloud-square) anymore.
See [Spring Documentation](https://docs.spring.io/spring-framework/docs/6.0.0/reference/html/integration.html#rest-http-interface)
for more details.

Here is an example:

<details>
  <summary>App.java</summary>

```java

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(App.class, args);
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

@HttpExchange("https://my-json-server.typicode.com")
interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}
```

</details>

_**So what is the problem ? 🤔**_

1. No auto configuration

   There's no autoconfigure for the clients, you need to create client beans manually. This is very painful if you have
   many clients.

   If you are familiar with `Spring Cloud OpenFeign`, you will find `@EnableFeignClients` is very useful, it reduces a
   lot of boilerplate code.

2. Not support Spring web annotations

   Native support for declarative HTTP clients is great, but it introduces a whole new set of annotations, such as
   `@GetExchange`, `@PostExchange`, etc. And does not support Spring web annotations, such as
   `@GetMapping`, `@PostMapping`, etc, which is extremely painful for users that using `Spring Cloud OpenFeign` and want
   to migrate to Spring 6.x.

**The main goal of this project is providing a `Spring Cloud OpenFeign` like experience for Spring 6.x declarative HTTP
clients and support Spring web annotations (`@GetMapping`, `@PostMapping`).**

## Quick Start

Add dependency:

```xml
<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>httpexchange-spring-boot-starter</artifactId>
    <version>3.1.1</version>
</dependency>
```

Write a classic Spring Boot application:

```java

@SpringBootApplication
@EnableExchangeClients
public class App {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(App.class, args);
        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }
}

@HttpExchange("https://my-json-server.typicode.com")
interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}
```

> No more boilerplate code! 🎉

## Features

### Autoconfigure Clients

Autoconfigure clients, all you need to do is adding the `@EnableExchangeClients` annotation. `@EnableExchangeClients` is
very similar to `@EnableFeignClients`.

It scans the clients that in the package of annotated class by default, you can also specify the packages to scan.

```java
@EnableExchangeClients(basePackages = "com.example")
```

> If you specify the packages to scan, it will only scan the specified packages, not including the package of annotated
> class.

You can also specify the clients to scan.

```java
@EnableExchangeClients(clients = {PostApi.class, UserApi.class})
```

> This is faster than scanning clients in the package.

You can also specify the clients and the packages to scan at the same time.

```java
@EnableExchangeClients(basePackages = "com.example", clients = {PostApi.class, UserApi.class})
```

> `Spring Cloud OpenFeign` does not support using `basePackages` and `clients` at the same time.

### Spring Web Annotations Support

Support to use spring web annotations to generate HTTP client, e.g. `@RequestMapping`, `@GetMapping`, `@PostMapping`
etc.

```java
@RequestMapping("/typicode/demo")
public interface PostApi {
    @GetMapping("/posts/{id}")
    Post getPost(@PathVariable int id);
}
```

### Dynamic Refresh Configuration

Support to dynamically refresh the configuration of clients, you can put the configuration in the configuration
center ([Consul](https://github.com/hashicorp/consul), [Apollo](https://github.com/apolloconfig/apollo), [Nacos](https://github.com/alibaba/nacos),
etc.), and change the configuration (e.g. `base-url`, `timeout`, `headers`), the client will be refreshed automatically
without restarting the application.

Use following configuration to enable this feature:

```yaml
http-exchange:
   refresh:
      enabled: true # default is false
```

> This feature needs `spring-cloud-context` in the classpath and a `RefreshEvent` was published.

### Configuration Driven

Providing a lot of configuration properties to customize the behavior of the client.

You can configure the `base-url`, `timeout` and `headers` for each channel, and each channel can apply to multiple clients.

```yaml
http-exchange:
  base-url: http://api-gateway          # global base-url
  response-timeout: 10000               # global timeout
  headers:                              # global headers
    - key: X-App-Name
      values: ${spring.application.name}
  refresh:
    enabled: true                       # enable dynamic refresh configuration
  channels:
    - base-url: http://order            # client specific base-url, will override global base-url
      response-timeout: 1000            # client specific timeout, will override global timeout
      headers:                          # client specific headers, will merge with global headers
        - key: X-Key
          values: [value1, value2]
      clients:                          # client to apply this channel
        - OrderApi             
    - base-url: user
      response-timeout: 2000
      clients:
        - UserApi
        - com.example.**.api.*          # Ant-style pattern
    - base-url: service-foo.namespace
      classes: [com.example.FooApi]     # client class to apply this channel
```

Using property `clients` or `classes` to identify the client, use `classes` first if configured, otherwise use `clients`.

For example, there is a client interface: `com.example.PostApi`, you can use following configuration to identify the client

```yaml
http-exchange:
  channels:
    - base-url: http://service
      clients: [com.example.PostApi] # Class canonical name
    # clients: [post-api] Class simple name (Kebab-case)
    # clients: [PostApi]  Class simple name (Pascal-case)
    # clients: [com.**.*Api] (Ant-style pattern)
      classes: [com.example.PostApi] # Class canonical name    
```

> configuration `clients` is more flexible, it supports Ant-style pattern, `classes` is more IDE-friendly and efficient.

### Url Variables

```java
@HttpExchange("${api.post.url}")
public interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}
```

### Validation

```java
@HttpExchange("${api.post.url}")
@Validated
public interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") @Min(1) @Max(3) int id);
}
```

> This feature needs `spring-boot` version >= 3.0.3,
> see [issue](https://github.com/spring-projects/spring-framework/issues/29782)
> and [tests](src/test/java/com/freemanan/starter/httpexchange/ValidationTests.java)

### Convert Java Bean to Query

In Spring Web/WebFlux (server side), it will automatically convert query string to Java Bean,
but `Spring Cloud OpenFeign` or `Exchange client of Spring 6` does not support to convert Java bean to query string by
default. In `Spring Cloud OpenFeign` you need `@SpringQueryMap` to achieve this feature.

`httpexhange-spring-boot-starter` supports this feature, and you don't need any additional annotations.

> In order not to change the default behavior of Spring, this feature is disabled by default,
> you can set `http-exchange.bean-to-query=true` to enable it.

```java
public interface PostApi {
  @GetExchange
  List<Post> findAll(Post condition);
}
```

Auto convert **non-null simple values** fields of `condition` to query string.

> Simple values: primitive/wrapper types, String, Date, etc.

### Customize Resolvers

```java
@Bean
HttpServiceArgumentResolver yourHttpServiceArgumentResolver() {
  return new YourHttpServiceArgumentResolver();
}

@Bean
StringValueResolver yourStringValueResolver() {
  return new YourStringValueResolver();
}
```

Auto-detect all of the `HttpServiceArgumentResolver` beans and `StringValueResolver` (only one), then apply them to build
the `HttpServiceProxyFactory`.


## Version

This project should work with any version of Spring Boot 3.

| Spring Boot | httpexchange-spring-boot-starter |
|-------------|----------------------------------|
| 3.x         | 3.1.1                            |

> Please always use the latest version!
