package br.local.scanbridge.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RegistrationService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String passwordHash = passwordEncoder.encode(request.getPassword());
        try {
            jdbcTemplate.update(
                    "INSERT INTO users(username, password, enabled) VALUES (?, ?, 1)",
                    email,
                    passwordHash
            );
            jdbcTemplate.update(
                    "INSERT INTO authorities(username, authority) VALUES (?, 'ROLE_USER')",
                    email
            );
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }
}
