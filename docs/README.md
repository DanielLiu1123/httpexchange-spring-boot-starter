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
@HttpExchange("https://my-json-server.typicode.com")
interface PostApi {
   @GetExchange("/typicode/demo/posts/{id}")
   Post getPost(@PathVariable("id") int id);
}

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(App.class, args);
        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(RestClient.Builder builder) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build();
    }

    @Bean
    PostApi postApi(HttpServiceProxyFactory factory) {
        return factory.createClient(PostApi.class);
    }
}
```

</details>

So what is the problem? 🤔

- No auto configuration

  There's no autoconfiguration for the clients, you need to create client beans manually.
  This is excruciating if you have many clients.

  If you are familiar with `Spring Cloud OpenFeign`, you will find `@EnableFeignClients` is beneficial, it reduces a
  lot of boilerplate code.

- Not support Spring web annotations

  Native support for declarative HTTP clients is great, but it introduces a whole new set of annotations, such as
  `@GetExchange`, `@PostExchange`, etc. And does not support Spring web annotations, such as
  `@GetMapping`, `@PostMapping`, etc., which is extremely painful for users that using `Spring Cloud OpenFeign` and want
  to migrate to Spring 6.x.

The main goals of this project:
- Promote the use of `@HttpExchange` as a neutral annotation to define API interfaces.
- Provide a `Spring Cloud OpenFeign` like experience for Spring 6.x declarative HTTP clients.
- Support Spring web annotations (`@RequestMapping`, `@GetMapping`).
- Not introduce external annotations, easy to migrate to other implementations.

## Quick Start

Add dependency:

```groovy
implementation("io.github.danielliu1123:httpexchange-spring-boot-starter:3.2.1")
```

Write a classic Spring Boot application:

```java
@HttpExchange("/typicode/demo")
interface PostApi {
   record Post(Integer id, String title) {}

   @GetExchange("/posts/{id}")
   Post getPost(@PathVariable Integer id);
}

@SpringBootApplication
@EnableExchangeClients
public class App {
   public static void main(String[] args) {
      new SpringApplicationBuilder(QuickStartApp.class)
              .properties("http-exchange.base-url=https://my-json-server.typicode.com")
              .run(args);
   }

   @Autowired
   private PostApi api;

   @Bean
   ApplicationRunner runner() {
      return args -> api.getPost(1);
   }
}
```

> No more boilerplate code! 🎉

Configure the base URL for different clients:

```yaml
http-exchange:
   channels:
      - base-url: http://user-service
        clients:
           - com.example.user.api.**
           - com.example.api.user.**
      - base-url: http://order-service
        clients:
           - com.example.order.api.**
           - com.example.api.order.**
```

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

If you don't want to introduce external classes, you can achieve the same functionality using configuration:

```yaml
http-exchange:
   base-packages: com.example
   clients:
     - com.foo.PostApi
     - com.bar.UserApi
```

> If both configuration and annotations are used, the annotation value will be used first.


### Generate Base Implementation for Server

Generate base implementation for server, you can use the base implementation to implement the server side.

```groovy
annotationProcessor("io.github.danielliu1123:httpexchange-processor:3.2.1")
```

```java
@HttpExchange("/user")
public interface UserApi {
    @GetExchange("/{id}")
    UserDTO getUser(@PathVariable("id") String id);
}
```

Generated base implementation:

```java
public abstract class UserApiBase implements UserApi {
   @Override
   public UserDTO getUser(String id) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
   }
}
```

> Generated abstract class name is the interface name with suffix `Base` by default, can be configured by `httpexchange-processor.properties`.

Use the base implementation to implement the server side:

```java
@RestController
public class UserController extends UserApiBase {
   @Override
   public UserDTO getUser(String id) {
      return new UserDTO(id, "Freeman");
   }
}
```

#### Custom Configuration

You can create a `httpexchange-processor.properties` file in any directory and put configuration directives in it.
These apply to all source files in this directory and all child directories.

```properties
enabled=true
suffix=Base
prefix=
packages=com.example.api
outputSubpackage=generated
```

> `httpexchange-processor.properties` usually placed in project/module directory.
> Place it with `pom.xml` when using Maven, place it with `build.gradle` when using Gradle.

| Property         | Description                                                                                     | Default Value                              |
|------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------|
| enabled          | Enable the processor                                                                            | true                                       |
| suffix           | Generated base implementation class name suffix                                                 | Base (if suffix and prefix are both empty) |        
| prefix           | Generated base implementation class name prefix                                                 |                                            |
| packages         | Packages to scan, use comma to separate multiple packages, support `Ant-style` pattern          | All packages                               |
| outputSubpackage | Generated base implementation class output subpackage, relative to the package of the interface |                                            |

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

> Since 3.2.0, `@RequestMapping` support is disabled by default,
> you can set `http-exchange.request-mapping-support-enabled=true` to enable it.
>
> Consider using `@HttpExchange` instead of `@RequestMapping` if possible.

### Load Balancer Support

Support to work with `spring-cloud-starter-loadbalancer` to achieve client side load balancing.

Add dependency:

```groovy
implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
```

```yaml
http-exchange:
  channels:
    - base-url: user  # service id
      clients:
        - com.example.user.api.*Api
```

Load balancer will be enabled automatically when `spring-cloud-starter-loadbalancer` is in the classpath.

Disable load balancer for all channels:

```yaml
http-exchange:
  loadbalancer-enabled: false # default is true
```

Disable load balancer for a specific channel:

```yaml
http-exchange:
  channels:
    - base-url: user
      loadbalancer-enabled: false
      clients:
        - com.example.user.api.*Api
```

### Reactive Support

Support to define reactive API.

```java
@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    Mono<User> get(@PathVariable("id") String id);
    
    @GetExchange
    Flux<User> list();
}
```

### Set Read Timeout Dynamically

Dynamically set read timeouts per request.

- Use `RequestConfigurator`

    ```java
    @HttpExchange("/users")  
    interface UserApi extends RequestConfigurator<UserApi> {
        @GetExchange      
        List<User> list();
    }    
    
    @Service
    class UserService {
        @Autowired
        UserApi userApi;
        
        List<User> listWithTimeout(int timeout) {
            return userApi.withTimeout(timeout).list();
        }    
    }
    ```

  Each time the `RequestConfigurator` method is called, a new proxy client will be created, and it inherits the original configuration and will not affect the original configuration.

  > `RequestConfigurator` is suitable for client-side use but not for defining a neutral API. Therefore, a `Requester` is provided for a programmatic way to dynamically set the read timeout.

- Use `Requester`

    ```java
    List<User> users = Requester.create()
                            .withTimeout(10000)                              
                            .addHeader("X-Foo", "bar")                              
                            .call(() -> userApi.list());
    ```

> ⚠️ **Warning**: This feature needs to use `EnhancedJdkClientHttpRequestFactory` as the `ClientHttpRequestFactory` implementation, and this is the default behavior.

For `WebClient` client type, use `timeout` method to set the read timeout for each request.

```java
Flux<User> users = userApi.list().timeout(Duration.ofSeconds(10));
```

### Refresh Configuration Dynamically

Support to dynamically refresh the configuration of clients, you can put the configuration in the configuration
center ([Consul](https://github.com/hashicorp/consul), [Apollo](https://github.com/apolloconfig/apollo), [Nacos](https://github.com/alibaba/nacos),
etc.), and change the configuration (e.g. `base-url`, `timeout`, `headers`), the client will be refreshed automatically
without restarting the application.

Use the following configuration to enable this feature:

```yaml
http-exchange:
   refresh:
      enabled: true # default is false
```

> This feature needs `spring-cloud-context` in the classpath and a `RefreshEvent` was published.

### Configuration Driven

Providing a lot of configuration properties to customize the behavior of the client.
You can configure the `base-url`, `read-timeout` for each channel, and each channel can apply to multiple clients.

#### Basic Usage

```yaml
http-exchange:
  read-timeout: 5000
  channels:
    - base-url: http://user
      read-timeout: 3000
      clients:
        - com.example.user.api.*Api
    - base-url: http://order
      clients:
        - com.example.order.api.*Api
```

Explanation:

Set the global `read-timeout` to 5000ms.

Set first channel `base-url` to `http://user` and `read-timeout` to 3000ms(override global `read-timeout`),
apply to clients that satisfy the pattern `com.example.user.api.*Api`.

Set second channel `base-url` to `http://order`, apply to clients that satisfy the pattern `com.example.order.api.*Api`.

#### Client Matching Rules

Using property `clients` or `classes` to identify the client, use `classes` first if configured, otherwise use `clients`.

For example, there is a client interface: `com.example.PostApi`, you can use the following configuration to identify the client

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

> Configuration `clients` is more flexible, it supports Ant-style pattern, `classes` is more IDE-friendly and efficient.

See [example configuration](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/blob/main/httpexchange-spring-boot-autoconfigure/src/main/resources/application-http-exchange-statrer-example.yml) and [HttpExchangeProperties](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/blob/main/httpexchange-spring-boot-autoconfigure/src/main/java/io/github/danielliu1123/httpexchange/HttpExchangeProperties.java) for more details.

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

> ⚠️ **Warning**: This feature needs `spring-boot` version >= 3.0.3, see [issue](https://github.com/spring-projects/spring-framework/issues/29782).

### Convert Java Bean to Query

In Spring Web/WebFlux (server side), it will automatically convert query string to Java Bean,
but `Spring Cloud OpenFeign` or `Exchange client of Spring 6` does not support to convert Java bean to query string by
default. In `Spring Cloud OpenFeign` you need `@SpringQueryMap` to achieve this feature.

`httpexhange-spring-boot-starter` supports this feature, and you don't need any additional annotations.

> In order not to change the default behavior of Spring, this feature is disabled by default,
> you can set `http-exchange.bean-to-query-enabled=true` to enable it.

```java
public interface PostApi {
  @GetExchange
  List<Post> findAll(Post condition);
}
```

Auto convert **non-null simple values** fields of `condition` to query string.

> Simple values: primitive/wrapper types, String, etc.

### Customization

#### Add a custom HttpServiceArgumentResolver

```java
@Bean
HttpServiceArgumentResolver yourHttpServiceArgumentResolver() {
  return new YourHttpServiceArgumentResolver();
}
```

Auto-detect all of the `HttpServiceArgumentResolver` beans, then apply them to build the `HttpServiceProxyFactory`.

#### Change Client Type

There are many adapters for `HttpExchange` client: `RestClientAdapter`, `WebClientAdapter` and `RestTemplateAdapter`.

```yaml
http-exchange:
  client-type: REST_CLIENT
```

The framework will choose the appropriate adapter according to the http client interface.
If any method in the interface returns a reactive type (Mono/Flux),
then `WebClient` will be used, otherwise `RestClient` will be used.
In most cases, there's no need to explicitly specify the client type.

> ⚠️ **Warning**: The `connectTimeout` settings are not supported by `WEB_CLIENT`.

#### Change Http Client Implementation

For `RestClient` and `RestTemplate`, there are many built-in implementations of `ClientHttpRequestFactory`,
use `EnhancedJdkClientHttpRequestFactory` to support dynamic `read-timeout` by default.

> ⚠️ **Warning**:
> If choose to use other implementations, dynamic `read-timeout` for single request will not be supported,
> Spring does not provide an extension point to support this feature,
> see [issue](https://github.com/spring-projects/spring-framework/issues/31926)

```java
// Change ClientHttpRequestFactory for RestClient
@Bean
RestClientCustomizer restClientCustomizer() {
    return builder -> builder.requestFactory(new ReactorNettyClientRequestFactory());
}

// Change ClientHttpRequestFactory for RestTemplate
@Bean
RestTemplateCustomizer restTemplateCustomizer() {
    return restTemplate -> restTemplate.setRequestFactory(new ReactorNettyClientRequestFactory());
}

// Change ClientHttpConnector for WebClient
@Bean
WebClientCustomizer webClientCustomizer() {
    return builder -> builder.clientConnector(new ReactorClientHttpConnector());
}
```

## Version Compatibility

The project version aligns with Spring Boot 3.x.
For Spring Boot 3.2.x, use `httpexchange-spring-boot-starter` version 3.2.1.

| Spring Boot | httpexchange-spring-boot-starter |
|-------------|----------------------------------|
| 3.2.x       | 3.2.1                            |
| 3.1.x       | 3.1.8                            |
