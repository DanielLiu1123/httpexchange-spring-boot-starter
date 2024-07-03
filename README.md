# HttpExchange Spring Boot Starter [![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions) [![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/io.github.danielliu1123/httpexchange-spring-boot-starter) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Documentation](https://danielliu1123.github.io/httpexchange-spring-boot-starter/)

Spring 6 now directly supports creating HTTP clients with the [`@HttpExchange`](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface) annotation. 
This means you can use Spring instead of needing [Spring Cloud OpenFeign](https://github.com/spring-cloud/spring-cloud-openfeign).

The main goals of this project:

- Promote the use of `@HttpExchange` as a neutral annotation to define API interfaces.
- Provide a `Spring Cloud OpenFeign` like experience for Spring 6 declarative HTTP clients.
- Support Spring web annotations (`@RequestMapping`, `@GetMapping`).
- Not introduce external annotations, easy to migrate to other implementations.

## Quick Start

- Add dependency:

    ```groovy
    implementation("io.github.danielliu1123:httpexchange-spring-boot-starter:3.3.0")
    ```

- Write a classic Spring Boot application:

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

## Code of Conduct

This project is governed by the [Code of Conduct](./CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code of conduct.
Please report unacceptable behavior to llw599502537@gmail.com.

## Contributing

The [issue tracker](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues) is the preferred channel for bug reports, 
feature requests and submitting pull requests.

If you would like to contribute to the project, please refer to [Contributing](./CONTRIBUTING.md).

## License

The MIT License.

## Special Thanks

Many thanks to [JetBrains](https://www.jetbrains.com/) for sponsoring this Open Source project with a license.
