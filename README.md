# Http Exchange Spring Boot Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/com.freemanan/httpexchange-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The missing starter for Spring 6.x declarative http client.

## How to use

Gradle:

```groovy
implementation 'com.freemanan:httpexchange-spring-boot-starter:3.0.6'
```

Maven:

```xml

<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>httpexchange-spring-boot-starter</artifactId>
    <version>3.0.6</version>
</dependency>
```

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

You can simplify with `httpexhange-spring-boot-starter`:

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

- Automatically scan interfaces annotated with @HttpExchange and create corresponding beans.

  All you need to do is add the @EnableExchangeClients annotation to your main class.

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

## Version

The major/minor version of this project is consistent with the version of `Spring Boot`. Therefore, `3.0.x` of this
project
should work with `3.0.x` of `Spring Boot`. Please always use the latest version.

| Spring Boot | httpexchange-spring-boot-starter |
|-------------|----------------------------------|
| 3.0.x       | 3.0.6                            |

## License

The MIT License.
