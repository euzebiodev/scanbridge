package br.local.scanbridge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String publicUrl;
    private final String mailFrom;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${scanbridge.public-url}") String publicUrl,
            @Value("${scanbridge.mail.from}") String mailFrom
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.publicUrl = publicUrl;
        this.mailFrom = mailFrom;
    }

    public void requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!userExists(email)) {
            return;
        }

        String token = createToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);
        jdbcTemplate.update(
                """
                INSERT INTO password_reset_tokens(token_hash, username, expires_at, used_at, created_at)
                VALUES (?, ?, ?, NULL, ?)
                """,
                hashToken(token),
                email,
                expiresAt.toString(),
                now.toString()
        );

        sendResetEmail(email, token);
    }

    public boolean resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken());
        ResetToken storedToken;
        try {
            storedToken = jdbcTemplate.queryForObject(
                    """
                    SELECT username, expires_at, used_at
                    FROM password_reset_tokens
                    WHERE token_hash = ?
                    """,
                    (resultSet, rowNum) -> new ResetToken(
                            resultSet.getString("username"),
                            Instant.parse(resultSet.getString("expires_at")),
                            resultSet.getString("used_at")
                    ),
                    tokenHash
            );
        } catch (EmptyResultDataAccessException exception) {
            return false;
        }

        if (storedToken == null || storedToken.usedAt() != null || storedToken.expiresAt().isBefore(Instant.now())) {
            return false;
        }

        jdbcTemplate.update(
                "UPDATE users SET password = ? WHERE username = ?",
                passwordEncoder.encode(request.getPassword()),
                storedToken.username()
        );
        jdbcTemplate.update(
                "UPDATE password_reset_tokens SET used_at = ? WHERE token_hash = ?",
                Instant.now().toString(),
                tokenHash
        );
        return true;
    }

    private boolean userExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    private void sendResetEmail(String email, String token) {
        String link = publicUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("SMTP nao configurado. Link de recuperacao para {}: {}", email, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Recuperacao de senha - ScanBridge");
        message.setText("""
                Ola,

                Use o link abaixo para redefinir sua senha do ScanBridge. O link expira em 30 minutos.

                %s

                Se voce nao solicitou esta recuperacao, ignore este e-mail.
                """.formatted(link));
        mailSender.send(message);
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }

    private record ResetToken(String username, Instant expiresAt, String usedAt) {
    }
}
