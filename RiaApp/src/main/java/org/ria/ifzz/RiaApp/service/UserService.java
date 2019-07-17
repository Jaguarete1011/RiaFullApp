package org.ria.ifzz.RiaApp.service;

import org.ria.ifzz.RiaApp.domain.User;
import org.ria.ifzz.RiaApp.exception.UserNameAlreadyExistsException;
import org.ria.ifzz.RiaApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * @param newUser Instance of User class
     * @return new User object or ExceptionHandler if username already exists
     */
    public User saveUser(User newUser) throws UserNameAlreadyExistsException {

        try{
            newUser.setPassword(bCryptPasswordEncoder.encode(newUser.getPassword()));
            newUser.setUsername(newUser.getUsername());
            newUser.setConfirmPassword("");
            return userRepository.save(newUser);
        } catch (Exception ex) {
            throw new UserNameAlreadyExistsException("Username '" + newUser.getUsername() + "' already exists");
        }
    }
}
