package co.ravn.userdemo.controller;

import java.util.List;

import co.ravn.userdemo.config.Delay;
import co.ravn.userdemo.model.User;
import co.ravn.userdemo.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    private final Delay delay;

    public UserController(UserService userService, Delay delay) {
        this.userService = userService;
        this.delay = delay;
    }

    @GetMapping({"", "/"})
    public List<User> all() {
        LOGGER.debug("Received request to list all users");
        this.delay.delay();
        List<User> users = this.userService.listAll();
        LOGGER.debug("Returning {} users", users.size());
        return users;
    }

    @GetMapping({"/{id}",})
    public ResponseEntity<User> findById(@PathVariable Long id) {
        LOGGER.debug("Received request to find user with id={}", id);
        this.delay.delay();
        User user = this.userService.findWithId(id);
        if (user == null) {
            LOGGER.debug("User with id={} not found, returning 404", id);
            return ResponseEntity.notFound().build();
        }
        LOGGER.debug("Found user with id={}: {}", id, user.name());
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        LOGGER.info("Received request to delete user with id={}", id);
        this.delay.delay();
        try {
            this.userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("Failed to delete user with id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(path = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        LOGGER.info("Received request to create user with name='{}'", request.name());

        if (request.name() == null || request.name().trim().isEmpty()) {
            LOGGER.warn("Validation failed: name is null or empty");
            return ResponseEntity.badRequest().build();
        }

        this.delay.delay();

        try {
            User createdUser = this.userService.create(request.name());
            LOGGER.info("Successfully created user with id={}, name='{}'", createdUser.id(), createdUser.name());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            LOGGER.error("Failed to create user with name='{}'", request.name(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public record CreateUserRequest(String name) {}

}
