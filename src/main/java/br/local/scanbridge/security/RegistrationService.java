package br.local.scanbridge.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RegistrationService {

    private static final String PASSWORDLESS_MARKER = "{noop}PASSWORDLESS";

    private final JdbcTemplate jdbcTemplate;

    public RegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void register(RegistrationRequest request) {
        register(request.getEmail());
    }

    public void register(String emailAddress) {
        String email = normalize(emailAddress);
        try {
            jdbcTemplate.update(
                    "INSERT INTO users(username, password, enabled) VALUES (?, ?, 1)",
                    email,
                    PASSWORDLESS_MARKER
            );
            jdbcTemplate.update(
                    "INSERT INTO authorities(username, authority) VALUES (?, 'ROLE_USER')",
                    email
            );
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    public void ensureUser(String emailAddress) {
        String email = normalize(emailAddress);
        jdbcTemplate.update(
                """
                INSERT INTO users(username, password, enabled)
                VALUES (?, ?, 1)
                ON CONFLICT(username) DO UPDATE SET enabled = 1
                """,
                email,
                PASSWORDLESS_MARKER
        );
        jdbcTemplate.update(
                "INSERT OR IGNORE INTO authorities(username, authority) VALUES (?, 'ROLE_USER')",
                email
        );
    }

    public String normalize(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }
}
