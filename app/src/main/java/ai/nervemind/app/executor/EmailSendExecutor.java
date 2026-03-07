/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.executor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;

/**
 * Node executor for sending emails via SMTP.
 *
 * <p>
 * Sends emails using Jakarta Mail with configurable SMTP server settings.
 * Supports plain text and HTML content, CC/BCC recipients, and TLS encryption.
 * </p>
 *
 * <h2>Node Configuration</h2>
 * <table border="1">
 * <caption>Email Send node parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>smtpHost</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>SMTP server hostname</td>
 * </tr>
 * <tr>
 * <td>smtpPort</td>
 * <td>Integer</td>
 * <td>No</td>
 * <td>SMTP port (default: 587)</td>
 * </tr>
 * <tr>
 * <td>username</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>SMTP username</td>
 * </tr>
 * <tr>
 * <td>password</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>SMTP password</td>
 * </tr>
 * <tr>
 * <td>useTls</td>
 * <td>Boolean</td>
 * <td>No</td>
 * <td>Enable STARTTLS (default: true)</td>
 * </tr>
 * <tr>
 * <td>from</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Sender email address</td>
 * </tr>
 * <tr>
 * <td>to</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Comma-separated recipient addresses</td>
 * </tr>
 * <tr>
 * <td>cc</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Comma-separated CC addresses</td>
 * </tr>
 * <tr>
 * <td>bcc</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Comma-separated BCC addresses</td>
 * </tr>
 * <tr>
 * <td>subject</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Email subject line</td>
 * </tr>
 * <tr>
 * <td>body</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Email body content</td>
 * </tr>
 * <tr>
 * <td>bodyType</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Content type: text or html (default: text)</td>
 * </tr>
 * </table>
 *
 * <h2>Output Data</h2>
 * <ul>
 * <li><strong>success</strong> - Boolean indicating email was sent</li>
 * <li><strong>messageId</strong> - The generated Message-ID header</li>
 * <li><strong>sentAt</strong> - Timestamp when the email was sent</li>
 * <li><strong>recipientCount</strong> - Total number of recipients</li>
 * </ul>
 *
 * @since 1.1.0
 * @see NodeExecutor
 */
@Component
public class EmailSendExecutor implements NodeExecutor {

    private static final int DEFAULT_SMTP_PORT = 587;

    /**
     * Default constructor.
     */
    public EmailSendExecutor() {
        // Default constructor
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String smtpHost = getRequiredString(params, "smtpHost");
        int smtpPort = ((Number) params.getOrDefault("smtpPort", DEFAULT_SMTP_PORT)).intValue();
        String username = getRequiredString(params, "username");
        String password = getRequiredString(params, "password");
        boolean useTls = extractBoolean(params.get("useTls"), true);

        String from = getRequiredString(params, "from");
        String to = getRequiredString(params, "to");
        String cc = (String) params.getOrDefault("cc", "");
        String bcc = (String) params.getOrDefault("bcc", "");
        String subject = (String) params.getOrDefault("subject", "");
        String body = (String) params.getOrDefault("body", "");
        String bodyType = (String) params.getOrDefault("bodyType", "text");

        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG,
                "Email: sending via " + smtpHost + ":" + smtpPort + " to=" + to,
                Map.of());

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(smtpHost);
            mailSender.setPort(smtpPort);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            if (useTls) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "30000");
            props.put("mail.smtp.writetimeout", "10000");

            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(body, "html".equalsIgnoreCase(bodyType));

            String[] toAddresses = to.split(",");
            helper.setTo(toAddresses);
            int recipientCount = toAddresses.length;

            if (cc != null && !cc.isBlank()) {
                String[] ccAddresses = cc.split(",");
                helper.setCc(ccAddresses);
                recipientCount += ccAddresses.length;
            }
            if (bcc != null && !bcc.isBlank()) {
                String[] bccAddresses = bcc.split(",");
                helper.setBcc(bccAddresses);
                recipientCount += bccAddresses.length;
            }

            helper.setSentDate(new Date());
            mailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "Email: sent successfully, messageId=" + messageId,
                    Map.of());

            Map<String, Object> output = new HashMap<>();
            output.put("success", true);
            output.put("messageId", messageId);
            output.put("sentAt", new Date().toInstant().toString());
            output.put("recipientCount", recipientCount);
            return output;

        } catch (Exception e) {
            throw new NodeExecutionException("Email send failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getNodeType() {
        return "emailSend";
    }

    private boolean extractBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String getRequiredString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new NodeExecutionException("Required parameter '" + key + "' is missing or empty");
        }
        return value.toString();
    }
}
