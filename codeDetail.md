# Email System - Complete Project Structure

## Project Directory Structure
```
EmailSystem/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── emailsystem/
│                   ├── EmailSystemApplication.java
│                   ├── model/
│                   │   ├── Email.java
│                   │   └── User.java
│                   ├── server/
│                   │   ├── SMTPServer.java
│                   │   ├── POP3Server.java
│                   │   ├── protocol/
│                   │   │   ├── SMTPHandler.java
│                   │   │   └── POP3Handler.java
│                   │   └── storage/
│                   │       └── MailboxManager.java
│                   ├── client/
│                   │   ├── EmailClient.java
│                   │   ├── gui/
│                   │   │   ├── EmailMainFrame.java
│                   │   │   ├── InboxPanel.java
│                   │   │   ├── ComposePanel.java
│                   │   │   └── EmailViewPanel.java
│                   │   └── network/
│                   │       ├── SMTPClient.java
│                   │       └── POP3Client.java
│                   ├── util/
│                   │   ├── EmailValidator.java
│                   │   ├── MessageIdGenerator.java
│                   │   └── DateFormatter.java
│                   └── config/
│                       └── ServerConfig.java
├── resources/
│   ├── application.properties
│   ├── icons/
│   │   ├── email.png
│   │   ├── send.png
│   │   ├── refresh.png
│   │   └── compose.png
│   └── sample-data/
│       └── sample-emails.txt
├── lib/
├── docs/
│   ├── README.md
│   ├── API-Documentation.md
│   └── User-Guide.md
├── scripts/
│   ├── start-server.bat
│   ├── start-server.sh
│   ├── start-client.bat
│   └── start-client.sh
├── pom.xml (if using Maven)
├── build.gradle (if using Gradle)
└── README.md
```

## File Contents

### 1. EmailSystemApplication.java (Main Entry Point)
```java
package com.emailsystem;

import com.emailsystem.server.SMTPServer;
import com.emailsystem.server.POP3Server;
import com.emailsystem.client.gui.EmailMainFrame;
import com.emailsystem.config.ServerConfig;
import com.emailsystem.server.storage.MailboxManager;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Main application entry point for the Email System
 * Starts both SMTP and POP3 servers and launches the client GUI
 */
public class EmailSystemApplication {
    private static SMTPServer smtpServer;
    private static POP3Server pop3Server;
    private static MailboxManager mailboxManager;
    
    public static void main(String[] args) {
        System.out.println("Starting Email System...");
        
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
        
        // Initialize components
        initializeSystem();
        
        // Start servers asynchronously
        startServers();
        
        // Launch client GUI
        SwingUtilities.invokeLater(() -> {
            String defaultUser = args.length > 0 ? args[0] : "user@example.com";
            new EmailMainFrame(defaultUser).setVisible(true);
        });
        
        // Add shutdown hook
        addShutdownHook();
    }
    
    private static void initializeSystem() {
        mailboxManager = new MailboxManager();
        smtpServer = new SMTPServer(ServerConfig.SMTP_PORT, mailboxManager);
        pop3Server = new POP3Server(ServerConfig.POP3_PORT, mailboxManager);
    }
    
    private static void startServers() {
        CompletableFuture.runAsync(() -> smtpServer.start());
        CompletableFuture.runAsync(() -> pop3Server.start());
        
        // Wait for servers to initialize
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Email System...");
            if (smtpServer != null) smtpServer.stop();
            if (pop3Server != null) pop3Server.stop();
        }));
    }
    
    public static MailboxManager getMailboxManager() {
        return mailboxManager;
    }
}
```

### 2. model/Email.java
```java
package com.emailsystem.model;

import com.emailsystem.util.MessageIdGenerator;
import java.util.Date;
import java.util.Objects;

/**
 * Represents an email message with all its properties
 */
public class Email {
    public enum Status {
        DRAFT, SENT, DELIVERED, READ, FAILED
    }
    
    private final String messageId;
    private final String sender;
    private final String recipient;
    private final String subject;
    private final String body;
    private final Date timestamp;
    private Status status;
    private final String contentType;
    
    public Email(String sender, String recipient, String subject, String body) {
        this.messageId = MessageIdGenerator.generate();
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject != null ? subject : "";
        this.body = body != null ? body : "";
        this.timestamp = new Date();
        this.status = Status.DRAFT;
        this.contentType = "text/plain";
    }
    
    // Getters
    public String getMessageId() { return messageId; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Date getTimestamp() { return timestamp; }
    public Status getStatus() { return status; }
    public String getContentType() { return contentType; }
    
    // Setters
    public void setStatus(Status status) { 
        this.status = Objects.requireNonNull(status); 
    }
    
    @Override
    public String toString() {
        return String.format("Message-ID: %s\nFrom: %s\nTo: %s\nSubject: %s\nDate: %s\nStatus: %s\n\n%s",
                messageId, sender, recipient, subject, timestamp, status, body);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email email = (Email) obj;
        return Objects.equals(messageId, email.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
```

### 3. model/User.java
```java
package com.emailsystem.model;

import java.util.Objects;

/**
 * Represents a user in the email system
 */
public class User {
    private final String emailAddress;
    private final String username;
    private String password; // In production, this should be hashed
    private boolean isActive;
    
    public User(String emailAddress, String username, String password) {
        this.emailAddress = Objects.requireNonNull(emailAddress);
        this.username = Objects.requireNonNull(username);
        this.password = password;
        this.isActive = true;
    }
    
    public User(String emailAddress) {
        this(emailAddress, extractUsernameFromEmail(emailAddress), "password");
    }
    
    private static String extractUsernameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
    
    // Getters and setters
    public String getEmailAddress() { return emailAddress; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(emailAddress, user.emailAddress);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(emailAddress);
    }
    
    @Override
    public String toString() {
        return String.format("User{email='%s', username='%s', active=%s}", 
                           emailAddress, username, isActive);
    }
}
```

### 4. server/SMTPServer.java
```java
package com.emailsystem.server;

import com.emailsystem.server.protocol.SMTPHandler;
import com.emailsystem.server.storage.MailboxManager;
import com.emailsystem.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SMTP Server implementation for handling outgoing emails
 */
public class SMTPServer {
    private final int port;
    private final MailboxManager mailboxManager;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public SMTPServer(int port, MailboxManager mailboxManager) {
        this.port = port;
        this.mailboxManager = mailboxManager;
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.SMTP_THREAD_POOL_SIZE);
        this.running = false;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("SMTP Server started on port " + port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new SMTPHandler(clientSocket, mailboxManager));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting SMTP client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start SMTP server: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error stopping SMTP server: " + e.getMessage());
            threadPool.shutdownNow();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
```

### 5. server/POP3Server.java
```java
package com.emailsystem.server;

import com.emailsystem.server.protocol.POP3Handler;
import com.emailsystem.server.storage.MailboxManager;
import com.emailsystem.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * POP3 Server implementation for handling email retrieval
 */
public class POP3Server {
    private final int port;
    private final MailboxManager mailboxManager;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public POP3Server(int port, MailboxManager mailboxManager) {
        this.port = port;
        this.mailboxManager = mailboxManager;
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.POP3_THREAD_POOL_SIZE);
        this.running = false;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("POP3 Server started on port " + port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new POP3Handler(clientSocket, mailboxManager));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting POP3 client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start POP3 server: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error stopping POP3 server: " + e.getMessage());
            threadPool.shutdownNow();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
```

### 6. server/protocol/SMTPHandler.java
```java
package com.emailsystem.server.protocol;

import com.emailsystem.model.Email;
import com.emailsystem.server.storage.MailboxManager;
import com.emailsystem.util.EmailValidator;

import java.io.*;
import java.net.Socket;

/**
 * Handles SMTP protocol communication with clients
 */
public class SMTPHandler implements Runnable {
    private final Socket clientSocket;
    private final MailboxManager mailboxManager;
    private BufferedReader reader;
    private PrintWriter writer;
    
    public SMTPHandler(Socket clientSocket, MailboxManager mailboxManager) {
        this.clientSocket = clientSocket;
        this.mailboxManager = mailboxManager;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            handleSMTPSession();
            
        } catch (IOException e) {
            System.err.println("Error in SMTP handler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void handleSMTPSession() throws IOException {
        writer.println("220 Email Server Ready");
        
        String line;
        String sender = null;
        String recipient = null;
        StringBuilder emailData = new StringBuilder();
        boolean dataMode = false;
        
        while ((line = reader.readLine()) != null) {
            if (dataMode) {
                if (".".equals(line.trim())) {
                    processEmailData(sender, recipient, emailData.toString());
                    writer.println("250 OK: Message accepted for delivery");
                    dataMode = false;
                    emailData.setLength(0);
                } else {
                    emailData.append(line).append("\n");
                }
            } else {
                if (line.startsWith("HELO") || line.startsWith("EHLO")) {
                    writer.println("250 Hello");
                } else if (line.startsWith("MAIL FROM:")) {
                    sender = extractEmail(line);
                    if (EmailValidator.isValid(sender)) {
                        writer.println("250 OK");
                    } else {
                        writer.println("550 Invalid sender address");
                    }
                } else if (line.startsWith("RCPT TO:")) {
                    recipient = extractEmail(line);
                    if (EmailValidator.isValid(recipient)) {
                        writer.println("250 OK");
                    } else {
                        writer.println("550 Invalid recipient address");
                    }
                } else if (line.startsWith("DATA")) {
                    writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
                    dataMode = true;
                } else if (line.startsWith("QUIT")) {
                    writer.println("221 Goodbye");
                    break;
                } else if (line.startsWith("RSET")) {
                    sender = null;
                    recipient = null;
                    writer.println("250 OK");
                } else {
                    writer.println("500 Command not recognized");
                }
            }
        }
    }
    
    private String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return line.substring(start + 1, end);
        }
        String[] parts = line.split(":");
        return parts.length > 1 ? parts[1].trim() : "";
    }
    
    private void processEmailData(String sender, String recipient, String data) {
        if (sender == null || recipient == null) {
            writer.println("503 Bad sequence - need MAIL FROM and RCPT TO");
            return;
        }
        
        String[] lines = data.split("\n");
        String subject = "";
        StringBuilder body = new StringBuilder();
        boolean inHeaders = true;
        
        for (String line : lines) {
            if (inHeaders) {
                if (line.trim().isEmpty()) {
                    inHeaders = false;
                } else if (line.startsWith("Subject:")) {
                    subject = line.substring(8).trim();
                }
            } else {
                body.append(line).append("\n");
            }
        }
        
        Email email = new Email(sender, recipient, subject, body.toString().trim());
        email.setStatus(Email.Status.SENT);
        mailboxManager.deliverEmail(email);
        
        System.out.println("Email delivered: " + sender + " -> " + recipient);
    }
    
    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing SMTP connection: " + e.getMessage());
        }
    }
}
```

### 7. server/protocol/POP3Handler.java
```java
package com.emailsystem.server.protocol;

import com.emailsystem.model.Email;
import com.emailsystem.server.storage.MailboxManager;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Handles POP3 protocol communication with clients
 */
public class POP3Handler implements Runnable {
    private final Socket clientSocket;
    private final MailboxManager mailboxManager;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentUser;
    private boolean authenticated;
    
    public POP3Handler(Socket clientSocket, MailboxManager mailboxManager) {
        this.clientSocket = clientSocket;
        this.mailboxManager = mailboxManager;
        this.authenticated = false;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            handlePOP3Session();
            
        } catch (IOException e) {
            System.err.println("Error in POP3 handler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void handlePOP3Session() throws IOException {
        writer.println("+OK POP3 server ready");
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            String command = parts[0].toUpperCase();
            
            switch (command) {
                case "USER":
                    handleUserCommand(parts);
                    break;
                case "PASS":
                    handlePassCommand(parts);
                    break;
                case "STAT":
                    handleStatCommand();
                    break;
                case "LIST":
                    handleListCommand(parts);
                    break;
                case "RETR":
                    handleRetrCommand(parts);
                    break;
                case "DELE":
                    handleDeleCommand(parts);
                    break;
                case "QUIT":
                    writer.println("+OK Goodbye");
                    return;
                case "NOOP":
                    writer.println("+OK");
                    break;
                default:
                    writer.println("-ERR Unknown command");
            }
        }
    }
    
    private void handleUserCommand(String[] parts) {
        if (parts.length > 1) {
            currentUser = parts[1];
            writer.println("+OK User accepted");
        } else {
            writer.println("-ERR Invalid user command");
        }
    }
    
    private void handlePassCommand(String[] parts) {
        // Simple authentication - in production, implement proper auth
        if (currentUser != null) {
            authenticated = true;
            writer.println("+OK Password accepted");
        } else {
            writer.println("-ERR Authentication failed");
        }
    }
    
    private void handleStatCommand() {
        if (!authenticated) {
            writer.println("-ERR Not authenticated");
            return;
        }
        
        List<Email> emails = mailboxManager.getEmails(currentUser);
        int totalSize = emails.stream().mapToInt(e -> e.toString().length()).sum();
        writer.println("+OK " + emails.size() + " " + totalSize);
    }
    
    private void handleListCommand(String[] parts) {
        if (!authenticated) {
            writer.println("-ERR Not authenticated");
            return;
        }
        
        List<Email> emails = mailboxManager.getEmails(currentUser);
        
        if (parts.length > 1) {
            // List specific message
            try {
                int msgNum = Integer.parseInt(parts[1]) - 1;
                if (msgNum >= 0 && msgNum < emails.size()) {
                    Email email = emails.get(msgNum);
                    writer.println("+OK " + (msgNum + 1) + " " + email.toString().length());
                } else {
                    writer.println("-ERR No such message");
                }
            } catch (NumberFormatException e) {
                writer.println("-ERR Invalid message number");
            }
        } else {
            // List all messages
            writer.println("+OK " + emails.size() + " messages");
            for (int i = 0; i < emails.size(); i++) {
                writer.println((i + 1) + " " + emails.get(i).toString().length());
            }
            writer.println(".");
        }
    }
    
    private void handleRetrCommand(String[] parts) {
        if (!authenticated) {
            writer.println("-ERR Not authenticated");
            return;
        }
        
        if (parts.length > 1) {
            try {
                int msgNum = Integer.parseInt(parts[1]) - 1;
                List<Email> emails = mailboxManager.getEmails(currentUser);
                
                if (msgNum >= 0 && msgNum < emails.size()) {
                    Email email = emails.get(msgNum);
                    writer.println("+OK " + email.toString().length() + " octets");
                    writer.println(email.toString());
                    writer.println(".");
                    mailboxManager.markAsRead(currentUser, email.getMessageId());
                } else {
                    writer.println("-ERR No such message");
                }
            } catch (NumberFormatException e) {
                writer.println("-ERR Invalid message number");
            }
        } else {
            writer.println("-ERR Missing message number");
        }
    }
    
    private void handleDeleCommand(String[] parts) {
        if (!authenticated) {
            writer.println("-ERR Not authenticated");
            return;
        }
        
        if (parts.length > 1) {
            try {
                int msgNum = Integer.parseInt(parts[1]) - 1;
                List<Email> emails = mailboxManager.getEmails(currentUser);
                
                if (msgNum >= 0 && msgNum < emails.size()) {
                    Email email = emails.get(msgNum);
                    mailboxManager.deleteEmail(currentUser, email.getMessageId());
                    writer.println("+OK Message deleted");
                } else {
                    writer.println("-ERR No such message");
                }
            } catch (NumberFormatException e) {
                writer.println("-ERR Invalid message number");
            }
        } else {
            writer.println("-ERR Missing message number");
        }
    }
    
    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing POP3 connection: " + e.getMessage());
        }
    }
}
```

### 8. server/storage/MailboxManager.java
```java
package com.emailsystem.server.storage;

import com.emailsystem.model.Email;
import com.emailsystem.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages email storage and retrieval for all users
 */
public class MailboxManager {
    private final Map<String, List<Email>> mailboxes;
    private final Map<String, User> users;
    
    public MailboxManager() {
        this.mailboxes = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        // Create sample users
        createUser(new User("user1@example.com"));
        createUser(new User("user2@example.com"));
        createUser(new User("admin@example.com"));
        
        // Add sample emails
        Email sample1 = new Email("admin@example.com", "user1@example.com",
                "Welcome to Email System",
                "Welcome to our advanced email system!\n\nThis is a sample email to demonstrate the functionality.\n\nBest regards,\nAdmin Team");
        sample1.setStatus(Email.Status.DELIVERED);
        deliverEmail(sample1);
        
        Email sample2 = new Email("newsletter@tech.com", "user2@example.com",
                "Weekly Tech Newsletter",
                "Here are the latest updates in technology:\n\n1. Java 21 LTS released with new features\n2. Spring Boot 3.0 now available\n3. AI and Machine Learning advances\n\nStay updated!\nTech Newsletter Team");
        sample2.setStatus(Email.Status.DELIVERED);
        deliverEmail(sample2);
    }
    
    public synchronized void createUser(User user) {
        users.put(user.getEmailAddress(), user);
        mailboxes.putIfAbsent(user.getEmailAddress(), new ArrayList<>());
    }
    
    public synchronized void deliverEmail(Email email) {
        String recipient = email.getRecipient();
        
        // Create user if doesn't exist
        if (!users.containsKey(recipient)) {
            createUser(new User(recipient));
        }
        
        mailboxes.computeIfAbsent(recipient, k -> new ArrayList<>()).add(email);
        email.setStatus(Email.Status.DELIVERED);
        
        System.out.println("Email delivered to " + recipient + " - Subject: " + email.getSubject());
    }
    
    public synchronized List<Email> getEmails(String userEmail) {
        return new ArrayList<>(mailboxes.getOrDefault(userEmail, new ArrayList<>()));
    }
    
    public synchronized void markAsRead(String userEmail, String messageId) {
        List<Email> userEmails = mailboxes.get(userEmail);
        if (userEmails != null) {
            for (Email email : userEmails) {
                if (email.getMessageId().equals(messageId)) {
                    email.setStatus(Email.Status.READ);
                    break;
                }
            }
        }
    }
    
    public synchronized boolean deleteEmail(String userEmail, String messageId) {
        List<Email> userEmails = mailboxes.get(userEmail);
        if (userEmails != null) {
            return userEmails.removeIf(email -> email.getMessageId().equals(messageId));
        }
        return false;
    }
    
    public synchronized int getEmailCount(String userEmail) {
        List<Email> emails = mailboxes.get(userEmail);
        return emails != null ? emails.size() : 0;
    }
    
    public synchronized User getUser(String emailAddress) {
        return users.get(emailAddress);
    }
    
    public synchronized Set<String> getAllUsers() {
        return new HashSet<>(users.keySet());
    }
}
```

### 9. client/gui/EmailMainFrame.java
```java
package com.emailsystem.client.gui;

import com.emailsystem.EmailSystemApplication;
import com.emailsystem.model.Email;
import com.emailsystem.server.storage.MailboxManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main GUI frame for the email client
 */
public class EmailMainFrame extends JFrame {
    private final String currentUser;
    private final MailboxManager mailboxManager;
    private InboxPanel inboxPanel;
    private ComposePanel composePanel;
    private EmailViewPanel emailViewPanel;
    private Timer refreshTimer;
    
    public EmailMainFrame(String user) {
        this.currentUser = user;
        this.mailboxManager = EmailSystemApplication.getMailboxManager();
        initializeGUI();
        startRefreshTimer();
    }
    
    private void initializeGUI() {
        setTitle("Email Client - " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
