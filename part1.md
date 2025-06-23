# EmailSystem - Part 1: Core Models and Configuration

 Let me start with Part 1, which includes the main application, model classes, and configuration.Here's **Part 1** of the Email System code, which includes:

## Files Created:

1. **EmailSystemApplication.java** - Main application entry point that can start either server or client mode
2. **Email.java** - Email model class with all email properties and methods
3. **User.java** - User model class with authentication and email management
4. **ServerConfig.java** - Configuration management class with default settings
5. **application.properties** - Configuration file with server and client settings

## Key Features in Part 1:

- **EmailSystemApplication**: Entry point with server/client mode selection
- **Email Model**: Complete email representation with headers, body, timestamps, and utility methods
- **User Model**: User management with inbox, sent, drafts, trash folders and authentication
- **ServerConfig**: Centralized configuration with properties file support
- **Properties File**: Configurable settings for ports, timeouts, and other parameters

The Email and User models include comprehensive functionality for email management, authentication, and folder operations. The configuration system allows for flexible deployment settings.

## src/main/java/com/emailsystem/EmailSystemApplication.java
```java
package com.emailsystem;

import com.emailsystem.server.SMTPServer;
import com.emailsystem.server.POP3Server;
import com.emailsystem.client.EmailClient;
import com.emailsystem.config.ServerConfig;

public class EmailSystemApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java EmailSystemApplication [server|client]");
            System.exit(1);
        }
        
        String mode = args[0].toLowerCase();
        
        switch (mode) {
            case "server":
                startServer();
                break;
            case "client":
                startClient();
                break;
            default:
                System.out.println("Invalid mode. Use 'server' or 'client'");
                System.exit(1);
        }
    }
    
    private static void startServer() {
        System.out.println("Starting Email Server...");
        
        // Start SMTP Server
        SMTPServer smtpServer = new SMTPServer();
        Thread smtpThread = new Thread(smtpServer);
        smtpThread.start();
        
        // Start POP3 Server
        POP3Server pop3Server = new POP3Server();
        Thread pop3Thread = new Thread(pop3Server);
        pop3Thread.start();
        
        System.out.println("Email Server started successfully!");
        System.out.println("SMTP Server running on port " + ServerConfig.SMTP_PORT);
        System.out.println("POP3 Server running on port " + ServerConfig.POP3_PORT);
        
        // Keep main thread alive
        try {
            smtpThread.join();
            pop3Thread.join();
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
        }
    }
    
    private static void startClient() {
        System.out.println("Starting Email Client...");
        javax.swing.SwingUtilities.invokeLater(() -> {
            new EmailClient().start();
        });
    }
}
```

## src/main/java/com/emailsystem/model/Email.java
```java
package com.emailsystem.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class Email {
    private String messageId;
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private LocalDateTime timestamp;
    private boolean read;
    private int size;
    
    public Email() {
        this.to = new ArrayList<>();
        this.cc = new ArrayList<>();
        this.bcc = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }
    
    public Email(String from, String to, String subject, String body) {
        this();
        this.from = from;
        this.to.add(to);
        this.subject = subject;
        this.body = body;
        this.size = calculateSize();
    }
    
    public Email(String from, List<String> to, String subject, String body) {
        this();
        this.from = from;
        this.to.addAll(to);
        this.subject = subject;
        this.body = body;
        this.size = calculateSize();
    }
    
    private int calculateSize() {
        int size = 0;
        if (from != null) size += from.length();
        if (to != null) size += to.stream().mapToInt(String::length).sum();
        if (cc != null) size += cc.stream().mapToInt(String::length).sum();
        if (subject != null) size += subject.length();
        if (body != null) size += body.length();
        return size;
    }
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getFrom() { return from; }
    public void setFrom(String from) { 
        this.from = from;
        this.size = calculateSize();
    }
    
    public List<String> getTo() { return new ArrayList<>(to); }
    public void setTo(List<String> to) { 
        this.to = new ArrayList<>(to);
        this.size = calculateSize();
    }
    
    public void addTo(String email) {
        this.to.add(email);
        this.size = calculateSize();
    }
    
    public List<String> getCc() { return new ArrayList<>(cc); }
    public void setCc(List<String> cc) { 
        this.cc = new ArrayList<>(cc);
        this.size = calculateSize();
    }
    
    public void addCc(String email) {
        this.cc.add(email);
        this.size = calculateSize();
    }
    
    public List<String> getBcc() { return new ArrayList<>(bcc); }
    public void setBcc(List<String> bcc) { 
        this.bcc = new ArrayList<>(bcc);
        this.size = calculateSize();
    }
    
    public void addBcc(String email) {
        this.bcc.add(email);
        this.size = calculateSize();
    }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { 
        this.subject = subject;
        this.size = calculateSize();
    }
    
    public String getBody() { return body; }
    public void setBody(String body) { 
        this.body = body;
        this.size = calculateSize();
    }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public int getSize() { return size; }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public String toRawMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message-ID: ").append(messageId).append("\r\n");
        sb.append("From: ").append(from).append("\r\n");
        sb.append("To: ").append(String.join(", ", to)).append("\r\n");
        if (!cc.isEmpty()) {
            sb.append("Cc: ").append(String.join(", ", cc)).append("\r\n");
        }
        sb.append("Subject: ").append(subject != null ? subject : "").append("\r\n");
        sb.append("Date: ").append(getFormattedTimestamp()).append("\r\n");
        sb.append("\r\n");
        sb.append(body != null ? body : "");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "Email{" +
                "from='" + from + '\'' +
                ", to=" + to +
                ", subject='" + subject + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
}
```

## src/main/java/com/emailsystem/model/User.java
```java
package com.emailsystem.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class User {
    private String username;
    private String email;
    private String password;
    private List<Email> inbox;
    private List<Email> sent;
    private List<Email> drafts;
    private List<Email> trash;
    private LocalDateTime lastLogin;
    private boolean isOnline;
    
    public User() {
        this.inbox = new ArrayList<>();
        this.sent = new ArrayList<>();
        this.drafts = new ArrayList<>();
        this.trash = new ArrayList<>();
        this.isOnline = false;
    }
    
    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Authentication methods
    public boolean authenticate(String password) {
        return this.password != null && this.password.equals(password);
    }
    
    public void login() {
        this.isOnline = true;
        this.lastLogin = LocalDateTime.now();
    }
    
    public void logout() {
        this.isOnline = false;
    }
    
    // Email management methods
    public void addToInbox(Email email) {
        this.inbox.add(0, email); // Add to beginning for latest first
    }
    
    public void addToSent(Email email) {
        this.sent.add(0, email);
    }
    
    public void addToDrafts(Email email) {
        this.drafts.add(0, email);
    }
    
    public void moveToTrash(Email email) {
        inbox.remove(email);
        sent.remove(email);
        drafts.remove(email);
        trash.add(0, email);
    }
    
    public void deleteFromTrash(Email email) {
        trash.remove(email);
    }
    
    public void restoreFromTrash(Email email) {
        if (trash.remove(email)) {
            inbox.add(0, email);
        }
    }
    
    public int getUnreadCount() {
        return (int) inbox.stream().filter(email -> !email.isRead()).count();
    }
    
    public List<Email> getUnreadEmails() {
        return inbox.stream().filter(email -> !email.isRead()).toList();
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public List<Email> getInbox() { return new ArrayList<>(inbox); }
    public void setInbox(List<Email> inbox) { this.inbox = new ArrayList<>(inbox); }
    
    public List<Email> getSent() { return new ArrayList<>(sent); }
    public void setSent(List<Email> sent) { this.sent = new ArrayList<>(sent); }
    
    public List<Email> getDrafts() { return new ArrayList<>(drafts); }
    public void setDrafts(List<Email> drafts) { this.drafts = new ArrayList<>(drafts); }
    
    public List<Email> getTrash() { return new ArrayList<>(trash); }
    public void setTrash(List<Email> trash) { this.trash = new ArrayList<>(trash); }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", inbox=" + inbox.size() + " emails" +
                ", unread=" + getUnreadCount() +
                ", isOnline=" + isOnline +
                '}';
    }
}
```

## src/main/java/com/emailsystem/config/ServerConfig.java
```java
package com.emailsystem.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    // Default ports
    public static final int SMTP_PORT = 2525;
    public static final int POP3_PORT = 11000;
    public static final String SERVER_HOST = "localhost";
    
    // Server settings
    public static final int MAX_CONNECTIONS = 10;
    public static final int CONNECTION_TIMEOUT = 300000; // 5 minutes
    public static final int MAX_MESSAGE_SIZE = 10485760; // 10MB
    public static final String MAIL_DIRECTORY = "mailboxes";
    
    // Protocol settings
    public static final String SMTP_GREETING = "220 Email System SMTP Server Ready";
    public static final String POP3_GREETING = "+OK Email System POP3 Server Ready";
    public static final String SERVER_NAME = "EmailSystem";
    
    // Authentication settings
    public static final boolean REQUIRE_AUTH = true;
    public static final int MAX_AUTH_ATTEMPTS = 3;
    
    // Configuration loaded from properties file
    private static Properties properties;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ServerConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Could not load application.properties: " + e.getMessage());
            // Use default values
        }
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // Getter methods for configuration values
    public static int getSmtpPort() {
        return getIntProperty("smtp.port", SMTP_PORT);
    }
    
    public static int getPop3Port() {
        return getIntProperty("pop3.port", POP3_PORT);
    }
    
    public static String getServerHost() {
        return getProperty("server.host", SERVER_HOST);
    }
    
    public static int getMaxConnections() {
        return getIntProperty("server.max.connections", MAX_CONNECTIONS);
    }
    
    public static int getConnectionTimeout() {
        return getIntProperty("server.connection.timeout", CONNECTION_TIMEOUT);
    }
    
    public static int getMaxMessageSize() {
        return getIntProperty("server.max.message.size", MAX_MESSAGE_SIZE);
    }
    
    public static String getMailDirectory() {
        return getProperty("server.mail.directory", MAIL_DIRECTORY);
    }
    
    public static boolean isAuthRequired() {
        return getBooleanProperty("server.require.auth", REQUIRE_AUTH);
    }
    
    public static int getMaxAuthAttempts() {
        return getIntProperty("server.max.auth.attempts", MAX_AUTH_ATTEMPTS);
    }
}
```

## resources/application.properties
```properties
# Server Configuration
server.host=localhost
smtp.port=2525
pop3.port=11000
server.max.connections=10
server.connection.timeout=300000
server.max.message.size=10485760
server.mail.directory=mailboxes

# Authentication
server.require.auth=true
server.max.auth.attempts=3

# Logging
logging.level=INFO
logging.file=email-system.log

# Client Configuration
client.auto.refresh.interval=30000
client.max.concurrent.connections=5
```
