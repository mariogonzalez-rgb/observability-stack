package co.ravn.userdemo.service;

import java.util.List;

import co.ravn.userdemo.model.User;
import co.ravn.userdemo.repository.UserRepository;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User create(String name) {
        LOGGER.info("Creating user with name='{}'", name);
        try {
            User user = this.userRepository.save(new User(null, name));
            LOGGER.info("User created successfully: id={}, name='{}'", user.id(), user.name());
            return user;
        } catch (Exception e) {
            LOGGER.error("Failed to create user with name='{}'", name, e);
            throw e;
        }
    }

    @Transactional
    public void delete(long id) {
        LOGGER.info("Deleting user with id={}", id);
        try {
            this.userRepository.deleteById(id);
            LOGGER.info("User deleted successfully: id={}", id);
        } catch (Exception e) {
            LOGGER.error("Failed to delete user with id={}", id, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<User> listAll() {
        LOGGER.debug("Querying database for all users");
        List<User> users = this.userRepository.findAll();
        LOGGER.info("Retrieved {} users from database", users.size());
        return users;
    }

    @Transactional(readOnly = true)
    public @Nullable User findWithId(long id) {
        LOGGER.debug("Querying database for user with id={}", id);
        User user = this.userRepository.findById(id).orElse(null);
        if (user != null) {
            LOGGER.info("Found user: id={}, name='{}'", user.id(), user.name());
        } else {
            LOGGER.info("No user found with id={}", id);
        }
        return user;
    }
}
