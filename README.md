# Sprout - AutoREST

Sprout is a powerful tool that automatically generates SpringBoot RESTful APIs from your JPA Entities.
It simplifies the process of creating REST endpoints by leveraging your existing JPA entities, allowing you to focus on
building your application rather than boilerplate code.

<br>
<p align="center">
    <img src="icon.svg">
</p>

## Project content
The project consists of three main modules:
- `sprout-annotations`: Contains the annotations used to mark JPA entities for which REST endpoints should be generated.
- `sprout-processor`: The annotation processor that generates the REST controllers, services, and repositories based on the annotated entities.
- `sprout-runtime`: (optional) Provides runtime support for features like unified error handling and method-level security.


## Getting Started

To get started with Sprout, just follow these simple steps:

### Prerequisites
- Java 21 or higher
- SpringBoot 3.5.x or higher

1. Add the Sprout-annotation dependency to your Maven project and the sprout-processor to the maven-compiler-plugin.
2. Annotate your JPA entities with the `@SproutRessource` annotation.
3. Run your SpringBoot application, and Sprout will automatically generate RESTful endpoints for your entities into your target folder.

### Maven
```xml
<dependency>
    <groupId>de.flix29</groupId>
    <artifactId>sprout-annotations</artifactId>
    <version>${sprout.version}</version>
</dependency>

<build>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>${java.version}</release>
                <parameters>true</parameters>
                <annotationProcessorPaths>
                    <path>
                        <groupId>de.flix29</groupId>
                        <artifactId>sprout-processor</artifactId>
                        <version>${sprout.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```
With this simple setup, you already got a RestController with five REST endpoints for each of your annotated entities:
- `GET /api/{entity}` - Retrieve all entities
- `GET /api/{entity}/{id}` - Retrieve a specific entity by ID
- `POST /api/{entity}` - Create a new entity
- `PUT /api/{entity}/{id}` - Update an existing entity by ID
- `DELETE /api/{entity}/{id}` - Delete an entity by ID

These controllers are supplied with a corresponding Service and Repository layer, following best practices for SpringBoot applications.

## Optional Features
Sprout also offers several optional features that you can enable by adding additional annotations to your entities:
- `@SproutPolicy` Lets you define custom access policies for your each endpoint.
- `@SproutId` Lets you define which field of your entity should be used as the ID field if you don't want to use the Database ID.

If you add the sprout-runtime dependency to your project, you can also use the following features:

```xml
<dependency>
    <groupId>de.flix29</groupId>
    <artifactId>sprout-runtime</artifactId>
    <version>${sprout.version}</version>
</dependency>
```

### Unified Error Handling
Sprout provides a unified error handling mechanism that returns consistent error responses for all endpoints.
This is enabled by default when you include the sprout-runtime dependency. It'll provide the following parameters which can be set via the application.properties file:
- `sprout.errors.enabled` - Whether the error handling is enabled (default: true)
- `sprout.error.log-log-stacktraces` - Whether to include stack traces in error responses (default: false)
- `sprout.errors.internal-code` - The error code to log for internal server errors (default: internal-error)

### Method-Level Security
In addition to the `@SproutPolicy` annotation, you can use the `sprout.security.enabled` property to enable method-level security for your endpoints. This will use Spring Security to enforce access control based on the policies defined in your entities.

## Customizing Generated Endpoints

Every generated controller now looks for an optional override bean that can replace the default CRUD logic on a per-endpoint basis.
For an entity named `Order`, Sprout generates an interface called `SproutOrderControllerOverride` in the `generated.controllers` package. Implement this interface, register it as a Spring bean (for example with `@Component`), and return a custom `ResponseEntity` from the endpoints you want to replace. Returning `Optional.empty()` keeps the default behaviour for that method.
Replace `<your package>` with the package that contains your generated classes when importing the service.

```java
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

// import <your package>.generated.controllers.SproutOrderControllerOverride;
// import <your package>.generated.services.SproutOrderService;

@Component
public class OrderControllerOverride implements SproutOrderControllerOverride {

    @Override
    public Optional<ResponseEntity<List<Order>>> create(Order newOrder, SproutOrderService service) {
        // add custom business logic before falling back to the generated service
        if (newOrder.isExpress()) {
            return Optional.of(ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(service.save(newOrder)));
        }
        return Optional.empty();
    }
}
```

All override methods receive the generated service instance so you can reuse the default repository-backed implementation or compose it with domain-specific behaviour.
