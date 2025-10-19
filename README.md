# <img src="website/static/img/logo.png" width="80" height="80"> HttpExchange Spring Boot Starter [![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions) [![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/io.github.danielliu1123/httpexchange-spring-boot-starter) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Spring 6 now supports creating HTTP clients using the [`@HttpExchange`](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface) annotation.
This removes the need for [Spring Cloud OpenFeign](https://github.com/spring-cloud/spring-cloud-openfeign).

The main goals of this project are:

- To promote `@HttpExchange` as the standard for defining API interfaces.
- To offer an experience similar to `Spring Cloud OpenFeign` for declarative HTTP clients.
- To ensure compatibility with Spring web annotations like `@RequestMapping` and `@GetMapping`.
- To avoid external annotations, making it easier to switch to other implementations.

> [!NOTE]\
> Spring Boot 4.x has supported automatic registration of `@HttpExchange` interfaces. However, the current official implementation still suffers from unnecessary repetition and verbosity. 
> Therefore, this project will continue to be maintained, offering a cleaner, more elegant, and more ergonomic alternative until the Spring Boot team gets it right.

## Quick Start

Spring Boot >= 4.0.0:

```groovy
implementation("org.springframework.boot:spring-boot-starter-restclient") // use RestClient as underlying http client
// implementation("org.springframework.boot:spring-boot-starter-webclient") // use WebClient as underlying http client
implementation("io.github.danielliu1123:httpexchange-spring-boot-starter:<latest>")
```

Spring Boot < 4.0.0:

```groovy
implementation("io.github.danielliu1123:httpexchange-spring-boot-starter:3.5.5")
```

```java
@HttpExchange("https://my-json-server.typicode.com")
interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}

@SpringBootApplication
@EnableExchangeClients
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    ApplicationRunner runner(PostApi api) {
        return args -> api.getPost(1);
    }
}
```

Refer to [quick-start](examples/quick-start).

## Documentation

Go to [Reference Documentation](https://danielliu1123.github.io/httpexchange-spring-boot-starter/docs/intro) for more information.

## Code of Conduct

This project is governed by the [Code of Conduct](./CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code of conduct.
Please report unacceptable behavior to llw599502537@gmail.com.

## Contributing

Use the [issue tracker](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues) for bug reports, 
feature requests, and submitting pull requests.

If you would like to contribute to the project, please refer to [Contributing](./CONTRIBUTING.md).

## License

The MIT License.

## Special Thanks

Many thanks to [JetBrains](https://www.jetbrains.com/) for sponsoring this Open Source project with a license.
