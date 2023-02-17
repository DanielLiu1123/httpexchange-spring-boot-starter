# Http Exchange Spring Boot Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/com.freemanan/httpexchange-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The missing starter for Spring 6.x declarative http client.

## How to use

Gradle:

```groovy
implementation 'com.freemanan:httpexchange-spring-boot-starter:3.0.5'
```

Maven:

```xml

<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>httpexchange-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

## What is it

Spring 6.0 has provided its own support for declarative http clients.

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

`httpexhange-spring-boot-starter` will auto scan the `@HttpExchange` annotated interfaces and create the corresponding
beans.

## Core Features

- Auto scan `@HttpExchange` annotated interfaces and create the corresponding beans.

  All you need to do is to add `@EnableExchangeClients` annotation to your main class.

- Support url variables.

  ```java
  @HttpExchange("${api.post.url}")
  public interface PostApi {
      @GetExchange("/typicode/demo/posts/{id}")
      Post getPost(@PathVariable("id") int id);
  }
  ```

## License

The MIT License.
