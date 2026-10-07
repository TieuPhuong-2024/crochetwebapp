package org.crochet.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.exception.EmailException;
import org.crochet.service.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * EmailService class.
 * Sends email through Gmail SMTP. Active only when {@code app.email.provider=smtp};
 * otherwise {@link BrevoEmailService} is used.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "smtp")
public class EmailService implements EmailSender {
    private final JavaMailSender javaMailSender;

    /**
     * Constructor
     *
     * @param javaMailSender JavaMailSender
     */
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * Send email
     *
     * @param to      user's email
     * @param subject subject
     * @param content content
     * @throws IllegalStateException failed to send email
     * @throws RuntimeException      unsupported encoding
     */
    @Override
    @Async
    public void send(String to, String subject, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(content, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("thamphuong.crochet@gmail.com", "Little Crochet");
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error(ResultCode.MSG_FAILED_SEND_EMAIL.message(), e);
            throw new EmailException(ResultCode.MSG_FAILED_SEND_EMAIL.message(), e,
                    ResultCode.MSG_FAILED_SEND_EMAIL.code());
        } catch (UnsupportedEncodingException e) {
            log.error("Email encoding error", e);
            throw new EmailException("Email encoding error", e,
                    ResultCode.MSG_FAILED_SEND_EMAIL.code());
        }
    }
}

