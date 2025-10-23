package de.flix29.sprout.runtime.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Allows applications to customize the behaviour of the generated Sprout controllers without modifying the
 * generated source files. By providing a bean that implements this interface for a specific entity, the
 * application can override individual endpoint handlers while still having access to the default implementation
 * through the provided callbacks.
 *
 * @param <T>  the entity type handled by the controller
 * @param <ID> the identifier type of the entity
 */
public interface SproutControllerDelegate<T, ID> {

    /**
     * Handles {@code GET /api/{entity}} requests.
     *
     * @param findAll callback that executes the default lookup logic
     * @return the response to return from the controller
     */
    default ResponseEntity<List<T>> getAll(Supplier<List<T>> findAll) {
        return ResponseEntity.ok(findAll.get());
    }

    /**
     * Handles {@code GET /api/{entity}/{id}} requests.
     *
     * @param id       the entity identifier supplied by the client
     * @param findById callback that executes the default lookup logic
     * @return the response to return from the controller
     */
    default ResponseEntity<T> getById(ID id, Function<ID, Optional<T>> findById) {
        return findById.apply(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handles {@code POST /api/{entity}} requests.
     *
     * @param entity the new entity provided by the client
     * @param save   callback that executes the default persistence logic
     * @return the response to return from the controller
     */
    default ResponseEntity<T> create(T entity, Function<T, T> save) {
        return ResponseEntity.status(HttpStatus.CREATED).body(save.apply(entity));
    }

    /**
     * Handles {@code PUT /api/{entity}/{id}} requests.
     *
     * @param id      the entity identifier supplied by the client
     * @param entity  the updated entity provided by the client
     * @param update  callback that executes the default update logic
     * @return the response to return from the controller
     */
    default ResponseEntity<T> update(ID id, T entity, BiFunction<ID, T, Optional<T>> update) {
        return update.apply(id, entity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handles {@code DELETE /api/{entity}/{id}} requests.
     *
     * @param id     the entity identifier supplied by the client
     * @param delete callback that executes the default deletion logic
     * @return the response to return from the controller
     */
    default ResponseEntity<Void> delete(ID id, Function<ID, Boolean> delete) {
        return Boolean.TRUE.equals(delete.apply(id))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

