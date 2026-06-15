package com.balians.musicgen.email;

import com.balians.musicgen.email.template.RenderedEmail;
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
        return sendEmail(to, subject, body, null);
    }

    public boolean sendRenderedEmail(String to, RenderedEmail renderedEmail) {
        return sendEmail(
                to,
                renderedEmail.subject(),
                renderedEmail.plainTextBody(),
                renderedEmail.htmlBody()
        );
    }

    private boolean sendEmail(String to, String subject, String plainTextBody, String htmlBody) {
        log.info("SendGrid email attempt to={} subject={} from={} html={}", to, subject, properties.getFromAddress(), htmlBody != null);
        if (!hasText(properties.getApiKey())) {
            log.warn("SendGrid API key is not configured; skipping email send to={}", to);
            return false;
        }
        if (!hasText(to)) {
            log.warn("Attempted to send email with blank recipient");
            return false;
        }
        if (!hasText(plainTextBody) && !hasText(htmlBody)) {
            log.warn("Attempted to send email with empty body to={}", to);
            return false;
        }

        Email from = new Email(properties.getFromAddress(), properties.getFromName());
        Email toEmail = new Email(to);

        Content primaryContent;
        if (hasText(plainTextBody)) {
            primaryContent = new Content("text/plain", plainTextBody);
        } else {
            primaryContent = new Content("text/html", htmlBody);
        }

        Mail mail = new Mail(from, subject, toEmail, primaryContent);
        if (hasText(htmlBody) && hasText(plainTextBody)) {
            mail.addContent(new Content("text/html", htmlBody));
        }

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
            }
            log.info("SendGrid email accepted by provider to={} status={}", to, response.getStatusCode());
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send SendGrid email to={}", to, ex);
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
