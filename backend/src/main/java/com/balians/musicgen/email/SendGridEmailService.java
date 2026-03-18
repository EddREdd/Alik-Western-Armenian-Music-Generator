package com.balians.musicgen.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendGridEmailService {

    private final SendGridEmailProperties properties;

    public boolean sendTextEmail(String to, String subject, String body) {
        log.info("SendGrid email attempt to={} subject={} from={}", to, subject, properties.getFromAddress());
        if (!hasText(properties.getApiKey())) {
            log.warn("SendGrid API key is not configured; skipping email send to={}", to);
            return false;
        }
        if (!hasText(to)) {
            log.warn("Attempted to send email with blank recipient");
            return false;
        }

        Email from = new Email(properties.getFromAddress(), properties.getFromName());
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(properties.getApiKey());
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                log.warn("SendGrid send failed status={} body={}", response.getStatusCode(), response.getBody());
                return false;
            } else {
                log.info("SendGrid email accepted by provider to={} status={}", to, response.getStatusCode());
                return true;
            }
        } catch (Exception ex) {
            log.warn("Failed to send SendGrid email to={}", to, ex);
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

