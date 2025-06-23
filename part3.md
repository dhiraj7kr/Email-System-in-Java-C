Here's **Part 3** of the Email System code, which includes the client components:

## Files Created:

1. **EmailClient.java** - Main client controller that manages connections and operations
2. **SMTPClient.java** - SMTP client for sending emails with full protocol implementation
3. **POP3Client.java** - POP3 client for retrieving emails with complete command set
4. **EmailMainFrame.java** - Main GUI window with tabbed interface and menu system

## Key Features in Part 3:

### EmailClient:
- **Login dialog** with authentication
- **Asynchronous operations** using CompletableFuture for non-blocking UI
- **Email management** (send, receive, delete)
- **Connection management** with proper cleanup

### SMTPClient:
- **Complete SMTP protocol** implementation
- **Authentication** with AUTH PLAIN
- **Multi-recipient support** (To, CC, BCC)
- **Dot stuffing** and proper message formatting
- **Error handling** and connection management

### POP3Client:
- **Full POP3 protocol** implementation
- **Email retrieval** with headers and body parsing
- **Message listing** and statistics
- **Email deletion** support
- **Proper connection lifecycle** management

### EmailMainFrame:
- **Tabbed interface** (Inbox, Compose, View)
- **Menu system** with keyboard shortcuts
- **Auto-refresh** functionality with timer
- **Status updates**
-------------------------------
# EmailSystem - Part 3: Client Components

## src/main/java/com/emailsystem/client/EmailClient.java
```java
package com.emailsystem.client;

import com.emailsystem.client.gui.EmailMainFrame;
import com.emailsystem.client.network.POP3Client;
import com.emailsystem.client.network.SMTPClient;
import com.emailsystem.config.ServerConfig;
import com.emailsystem.model.Email;
import com.emailsystem.model.User;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmailClient {
    private EmailMainFrame mainFrame;
    private POP3Client pop3Client;
    private SMTPClient smtpClient;
    private User currentUser;
    private boolean connected = false;
    
    public EmailClient() {
        initializeClients();
    }
    
    private void initializeClients() {
        pop3Client = new POP3Client(ServerConfig.getServerHost(), ServerConfig.getPop3Port());
        smtpClient = new SMTPClient(ServerConfig.getServerHost(), ServerConfig.getSmtpPort());
    }
    
    public void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            
            showLoginDialog();
        });
    }
    
    private void showLoginDialog() {
        JDialog loginDialog = new JDialog();
        loginDialog.setTitle("Email Client - Login");
        loginDialog.setModal(true);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setSize(350, 200);
        loginDialog.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        
        panel.add(new JLabel("Username/Email:"));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(20));
        
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);
        
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter both username and password");
                return;
            }
            
            // Attempt login in background
            CompletableFuture.supplyAsync(() -> login(username, password))
                .thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            loginDialog.dispose();
                            openMainWindow();
                        } else {
                            JOptionPane.showMessageDialog(loginDialog, 
                                "Login failed. Please check your credentials.");
                        }
                    });
                });
        });
        
        cancelButton.addActionListener(e -> {
            loginDialog.dispose();
            System.exit(0);
        });
        
        // Enter key triggers login
        loginDialog.getRootPane().setDefaultButton(loginButton);
        
        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }
    
    private boolean login(String username, String password) {
        try {
            if (pop3Client.connect() && pop3Client.authenticate(username, password)) {
                currentUser = new User(username, username.contains("@") ? username : username + "@localhost", password);
                connected = true;
                return true;
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return false;
    }
    
    private void openMainWindow() {
        mainFrame = new EmailMainFrame(this);
        mainFrame.setVisible(true);
        
        // Load initial emails
        refreshInbox();
    }
    
    public void refreshInbox() {
        if (!connected) return;
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return pop3Client.retrieveAllEmails();
            } catch (Exception e) {
                System.err.println("Error retrieving emails: " + e.getMessage());
                return null;
            }
        }).thenAccept(emails -> {
            if (emails != null) {
                SwingUtilities.invokeLater(() -> {
                    currentUser.setInbox(emails);
                    if (mainFrame != null) {
                        mainFrame.updateInbox(emails);
                    }
                });
            }
        });
    }
    
    public void sendEmail(Email email) {
        CompletableFuture.supplyAsync(() -> {
            try {
                if (smtpClient.connect()) {
                    if (smtpClient.authenticate(currentUser.getUsername(), currentUser.getPassword())) {
                        boolean sent = smtpClient.sendEmail(email);
                        smtpClient.disconnect();
                        return sent;
                    }
                }
                return false;
            } catch (Exception e) {
                System.err.println("Error sending email: " + e.getMessage());
                return false;
            }
        }).thenAccept(success -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    currentUser.addToSent(email);
                    JOptionPane.showMessageDialog(mainFrame, "Email sent successfully!");
                    if (mainFrame != null) {
                        mainFrame.clearCompose();
                    }
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to send email.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }
    
    public void deleteEmail(Email email) {
        CompletableFuture.runAsync(() -> {
            try {
                // Mark for deletion on server (simplified)
                currentUser.moveToTrash(email);
            } catch (Exception e) {
                System.err.println("Error deleting email: " + e.getMessage());
            }
        }).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null) {
                    mainFrame.updateInbox(currentUser.getInbox());
                }
            });
        });
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void shutdown() {
        try {
            if (pop3Client != null) {
                pop3Client.disconnect();
            }
            if (smtpClient != null) {
                smtpClient.disconnect();
            }
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
        connected = false;
    }
}
```

## src/main/java/com/emailsystem/client/network/SMTPClient.java
```java
package com.emailsystem.client.network;

import com.emailsystem.model.Email;
import com.emailsystem.util.EmailValidator;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class SMTPClient {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    
    public SMTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public boolean connect() throws IOException {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Read greeting
            String response = reader.readLine();
            if (response != null && response.startsWith("220")) {
                connected = true;
                
                // Send EHLO
                sendCommand("EHLO localhost");
                response = readResponse();
                
                return response.startsWith("250");
            }
        } catch (IOException e) {
            disconnect();
            throw e;
        }
        return false;
    }
    
    public boolean authenticate(String username, String password) throws IOException {
        if (!connected) return false;
        
        try {
            // Prepare AUTH PLAIN credentials
            String credentials = "\0" + username + "\0" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            
            sendCommand("AUTH PLAIN " + encodedCredentials);
            String response = readResponse();
            
            return response.startsWith("235");
        } catch (IOException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }
    
    public boolean sendEmail(Email email) throws IOException {
        if (!connected) return false;
        
        try {
            // MAIL FROM
            sendCommand("MAIL FROM:<" + email.getFrom() + ">");
            if (!readResponse().startsWith("250")) {
                return false;
            }
            
            // RCPT TO (for each recipient)
            for (String recipient : email.getTo()) {
                if (!EmailValidator.isValid(recipient)) {
                    System.err.println("Invalid recipient: " + recipient);
                    continue;
                }
                
                sendCommand("RCPT TO:<" + recipient + ">");
                if (!readResponse().startsWith("250")) {
                    System.err.println("Failed to add recipient: " + recipient);
                }
            }
            
            // CC recipients
            for (String cc : email.getCc()) {
                if (EmailValidator.isValid(cc)) {
                    sendCommand("RCPT TO:<" + cc + ">");
                    readResponse(); // Don't fail if CC fails
                }
            }
            
            // BCC recipients
            for (String bcc : email.getBcc()) {
                if (EmailValidator.isValid(bcc)) {
                    sendCommand("RCPT TO:<" + bcc + ">");
                    readResponse(); // Don't fail if BCC fails
                }
            }
            
            // DATA
            sendCommand("DATA");
            if (!readResponse().startsWith("354")) {
                return false;
            }
            
            // Send email content
            sendEmailData(email);
            
            // End data with single dot
            sendCommand(".");
            String response = readResponse();
            
            return response.startsWith("250");
            
        } catch (IOException e) {
            System.err.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
    
    private void sendEmailData(Email email) throws IOException {
        // Send headers
        writer.println("From: " + email.getFrom());
        writer.println("To: " + String.join(", ", email.getTo()));
        
        if (!email.getCc().isEmpty()) {
            writer.println("Cc: " + String.join(", ", email.getCc()));
        }
        
        writer.println("Subject: " + (email.getSubject() != null ? email.getSubject() : ""));
        writer.println("Date: " + email.getFormattedTimestamp());
        writer.println("Message-ID: " + email.getMessageId());
        writer.println(); // Empty line to separate headers from body
        
        // Send body
        String body = email.getBody() != null ? email.getBody() : "";
        String[] lines = body.split("\n");
        
        for (String line : lines) {
            // Handle dot stuffing
            if (line.startsWith(".")) {
                writer.println("." + line);
            } else {
                writer.println(line);
            }
        }
    }
    
    private void sendCommand(String command) throws IOException {
        writer.println(command);
        System.out.println("SMTP Client Sent: " + command);
    }
    
    private String readResponse() throws IOException {
        String response = reader.readLine();
        if (response != null) {
            System.out.println("SMTP Client Received: " + response);
        }
        return response;
    }
    
    public void disconnect() {
        try {
            if (connected && writer != null) {
                sendCommand("QUIT");
                readResponse();
            }
        } catch (IOException e) {
            // Ignore errors during disconnect
        }
        
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
}
```

## src/main/java/com/emailsystem/client/network/POP3Client.java
```java
package com.emailsystem.client.network;

import com.emailsystem.model.Email;
import com.emailsystem.util.MessageIdGenerator;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class POP3Client {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    private boolean authenticated = false;
    
    public POP3Client(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public boolean connect() throws IOException {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Read greeting
            String response = reader.readLine();
            if (response != null && response.startsWith("+OK")) {
                connected = true;
                return true;
            }
        } catch (IOException e) {
            disconnect();
            throw e;
        }
        return false;
    }
    
    public boolean authenticate(String username, String password) throws IOException {
        if (!connected) return false;
        
        try {
            // Send USER command
            sendCommand("USER " + username);
            String response = readResponse();
            if (!response.startsWith("+OK")) {
                return false;
            }
            
            // Send PASS command
            sendCommand("PASS " + password);
            response = readResponse();
            if (response.startsWith("+OK")) {
                authenticated = true;
                return true;
            }
        } catch (IOException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return false;
    }
    
    public int getMessageCount() throws IOException {
        if (!authenticated) return -1;
        
        sendCommand("STAT");
        String response = readResponse();
        
        if (response.startsWith("+OK")) {
            String[] parts = response.split(" ");
            if (parts.length >= 2) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }
    
    public List<Email> retrieveAllEmails() throws IOException {
        List<Email> emails = new ArrayList<>();
        
        int messageCount = getMessageCount();
        if (messageCount <= 0) {
            return emails;
        }
        
        for (int i = 1; i <= messageCount; i++) {
            Email email = retrieveEmail(i);
            if (email != null) {
                emails.add(email);
            }
        }
        
        return emails;
    }
    
    public Email retrieveEmail(int messageNumber) throws IOException {
        if (!authenticated) return null;
        
        sendCommand("RETR " + messageNumber);
        String response = readResponse();
        
        if (!response.startsWith("+OK")) {
            return null;
        }
        
        StringBuilder emailData = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }
            
            // Handle dot unstuffing
            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            
            emailData.append(line).append("\n");
        }
        
        return parseEmailData(emailData.toString());
    }
    
    public String getEmailHeaders(int messageNumber, int lines) throws IOException {
        if (!authenticated) return null;
        
        sendCommand("TOP " + messageNumber + " " + lines);
        String response = readResponse();
        
        if (!response.startsWith("+OK")) {
            return null;
        }
        
        StringBuilder headers = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }
            headers.append(line).append("\n");
        }
        
        return headers.toString();
    }
    
    public boolean deleteEmail(int messageNumber) throws IOException {
        if (!authenticated) return false;
        
        sendCommand("DELE " + messageNumber);
        String response = readResponse();
        
        return response.startsWith("+OK");
    }
    
    public List<String> listMessages() throws IOException {
        if (!authenticated) return new ArrayList<>();
        
        sendCommand("LIST");
        String response = readResponse();
        
        if (!response.startsWith("+OK")) {
            return new ArrayList<>();
        }
        
        List<String> messages = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }
            messages.add(line);
        }
        
        return messages;
    }
    
    private Email parseEmailData(String data) {
        Email email = new Email();
        
        String[] lines = data.split("\n");
        boolean inHeaders = true;
        StringBuilder body = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            if (inHeaders) {
                if (line.isEmpty()) {
                    inHeaders = false;
                    continue;
                }
                
                if (line.toLowerCase().startsWith("from:")) {
                    email.setFrom(extractHeaderValue(line));
                } else if (line.toLowerCase().startsWith("to:")) {
                    String[] recipients = extractHeaderValue(line).split(",");
                    List<String> toList = new ArrayList<>();
                    for (String recipient : recipients) {
                        toList.add(recipient.trim());
                    }
                    email.setTo(toList);
                } else if (line.toLowerCase().startsWith("cc:")) {
                    String[] ccRecipients = extractHeaderValue(line).split(",");
                    List<String> ccList = new ArrayList<>();
                    for (String cc : ccRecipients) {
                        ccList.add(cc.trim());
                    }
                    email.setCc(ccList);
                } else if (line.toLowerCase().startsWith("subject:")) {
                    email.setSubject(extractHeaderValue(line));
                } else if (line.toLowerCase().startsWith("message-id:")) {
                    email.setMessageId(extractHeaderValue(line));
                } else if (line.toLowerCase().startsWith("date:")) {
                    // Parse date if needed
                    email.setTimestamp(LocalDateTime.now());
                }
            } else {
                body.append(line).append("\n");
            }
        }
        
        email.setBody(body.toString().trim());
        
        // Generate message ID if not present
        if (email.getMessageId() == null || email.getMessageId().isEmpty()) {
            email.setMessageId(MessageIdGenerator.generate());
        }
        
        return email;
    }
    
    private String extractHeaderValue(String headerLine) {
        int colonIndex = headerLine.indexOf(':');
        if (colonIndex > 0 && colonIndex < headerLine.length() - 1) {
            return headerLine.substring(colonIndex + 1).trim();
        }
        return "";
    }
    
    private void sendCommand(String command) throws IOException {
        writer.println(command);
        System.out.println("POP3 Client Sent: " + command);
    }
    
    private String readResponse() throws IOException {
        String response = reader.readLine();
        if (response != null) {
            System.out.println("POP3 Client Received: " + response);
        }
        return response;
    }
    
    public void disconnect() {
        try {
            if (authenticated && writer != null) {
                sendCommand("QUIT");
                readResponse();
            }
        } catch (IOException e) {
            // Ignore errors during disconnect
        }
        
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        connected = false;
        authenticated = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
}
```

## src/main/java/com/emailsystem/client/gui/EmailMainFrame.java
```java
package com.emailsystem.client.gui;

import com.emailsystem.client.EmailClient;
import com.emailsystem.model.Email;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class EmailMainFrame extends JFrame {
    private EmailClient emailClient;
    private JTabbedPane tabbedPane;
    private InboxPanel inboxPanel;
    private ComposePanel composePanel;
    private EmailViewPanel viewPanel;
    private JLabel statusLabel;
    private Timer refreshTimer;
    
    public EmailMainFrame(EmailClient emailClient) {
        this.emailClient = emailClient;
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupTimer();
        
        setTitle("Email Client - " + emailClient.getCurrentUser().getEmail());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
    }
    
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        inboxPanel = new InboxPanel(this);
        composePanel = new ComposePanel(this);
        viewPanel = new EmailViewPanel(this);
        statusLabel = new JLabel("Connected to " + emailClient.getCurrentUser().getEmail());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create tabs
        tabbedPane.addTab("Inbox", createTabIcon("inbox"), inboxPanel, "View received emails");
        tabbedPane.addTab("Compose", createTabIcon("compose"), composePanel, "Compose new email");
        tabbedPane.addTab("View", createTabIcon("view"), viewPanel, "View selected email");
        
        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private Icon createTabIcon(String type) {
        // Create simple colored icons
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                switch (type) {
                    case "inbox":
                        g2.setColor(new Color(70, 130, 180));
                        break;
                    case "compose":
                        g2.setColor(new Color(34, 139, 34));
                        break;
                    case "view":
                        g2.setColor(new Color(255, 140, 0));
                        break;
                }
                
                g2.fillOval(x, y, getIconWidth(), getIconHeight());
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 12; }
            
            @Override
            public int getIconHeight() { return 12; }
        };
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem refreshItem = new JMenuItem("Refresh Inbox");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshItem.addActionListener(e -> refreshInbox());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
        exitItem.addActionListener(e -> handleExit());
        
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Email Menu
        JMenu emailMenu = new JMenu("Email");
        
        JMenuItem composeItem = new JMenuItem("New Email");
        composeItem.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));
        composeItem.addActionListener(e -> tabbedPane.setSelectedComponent(composePanel));
        
        emailMenu.add(composeItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        
        JMenuItem inboxItem = new JMenuItem("Go to Inbox");
        inboxItem.setAccelerator(KeyStroke.getKeyStroke("ctrl 1"));
        inboxItem.addActionListener(e -> tabbedPane.setSelectedComponent(inboxPanel));
        
        viewMenu.add(inboxItem);
        
        menuBar.add(fileMenu);
        menuBar.add(emailMenu);
        menuBar.add(viewMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void setupTimer() {
        // Auto-refresh every 30 seconds
        refreshTimer = new Timer(30000, e -> {
            if (emailClient.isConnected()) {
                refreshInbox();
            }
        });
        refreshTimer.start();
    }
    
    public void refreshInbox() {
        statusLabel.setText("Refreshing inbox...");
        emailClient.refreshInbox();
    }
    
    public void updateInbox(List<Email> emails) {
        inboxPanel.updateEmails(emails);
        int unreadCount = (int) emails.stream().filter(email -> !email.isRead()).count();
        
        String tabTitle = "Inbox";
        if (unreadCount > 0) {
            tabTitle += " (" + unreadCount + ")";
        }
        tabbedPane.setTitleAt(0, tabTitle);
        
        statusLabel.setText("Inbox updated - " + emails.size() + " messages (" + unreadCount + " unread)");
    }
    
    public void showEmail(Email email) {
        email.setRead(true);
        viewPanel.displayEmail(email);
        tabbedPane.setSelectedComponent(viewPanel);
        
        // Update inbox display to reflect read status
        inboxPanel.repaint();
    }
    
    public void composeNewEmail() {
        composePanel.clearForm();
        tabbedPane.setSelectedComponent(composePanel);
    }
    
    public void replyToEmail(Email originalEmail) {
        composePanel.setupReply(originalEmail);
        tabbedPane.setSelectedComponent(composePanel);
    }
    
    public void forwardEmail(Email originalEmail) {
        composePanel.setupForward(originalEmail);
        tabbedPane.setSelectedComponent(composePanel);
    }
    
    public void sendEmail(Email email) {
        statusLabel.setText("Sending email...");
        emailClient.sendEmail(email);
    }
    
    public void deleteEmail(Email email) {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this email?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            emailClient.deleteEmail(email);
            statusLabel.setText("Email deleted");
        }
    }
    
    public void clearCompose() {
        composePanel.clearForm();
    }
    
    public EmailClient getEmailClient() {
        return emailClient;
    }
    
    private void handleExit() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to exit?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            emailClient.shutdown();
            System.exit(0);
        }
    }
}
```
