import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

// Email data structure
class Email {
    private String messageId;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private Date timestamp;
    private EmailStatus status;
    private String contentType;
    
    public enum EmailStatus {
        DRAFT, SENT, DELIVERED, READ, FAILED
    }
    
    public Email(String sender, String recipient, String subject, String body) {
        this.messageId = generateMessageId();
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.timestamp = new Date();
        this.status = EmailStatus.DRAFT;
        this.contentType = "text/plain";
    }
    
    private String generateMessageId() {
        try {
            String input = System.currentTimeMillis() + Math.random() + "";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16);
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }
    
    // Getters and setters
    public String getMessageId() { return messageId; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Date getTimestamp() { return timestamp; }
    public EmailStatus getStatus() { return status; }
    public void setStatus(EmailStatus status) { this.status = status; }
    public String getContentType() { return contentType; }
    
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("From: %s\nTo: %s\nSubject: %s\nDate: %s\nStatus: %s\n\n%s",
                sender, recipient, subject, sdf.format(timestamp), status, body);
    }
}

// SMTP Server implementation
class SMTPServer {
    private final int port;
    private final Map<String, List<Email>> mailboxes;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public SMTPServer(int port) {
        this.port = port;
        this.mailboxes = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(10);
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
                    threadPool.submit(new SMTPHandler(clientSocket, this));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Error stopping SMTP server: " + e.getMessage());
        }
    }
    
    public synchronized void deliverEmail(Email email) {
        String recipient = email.getRecipient();
        mailboxes.computeIfAbsent(recipient, k -> new ArrayList<>()).add(email);
        email.setStatus(Email.EmailStatus.DELIVERED);
        System.out.println("Email delivered to " + recipient);
    }
    
    public synchronized List<Email> getEmails(String user) {
        return mailboxes.getOrDefault(user, new ArrayList<>());
    }
    
    public synchronized void markAsRead(String user, String messageId) {
        List<Email> userEmails = mailboxes.get(user);
        if (userEmails != null) {
            for (Email email : userEmails) {
                if (email.getMessageId().equals(messageId)) {
                    email.setStatus(Email.EmailStatus.READ);
                    break;
                }
            }
        }
    }
}

// SMTP Protocol Handler
class SMTPHandler implements Runnable {
    private final Socket clientSocket;
    private final SMTPServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    
    public SMTPHandler(Socket clientSocket, SMTPServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // SMTP greeting
            writer.println("220 Email Server Ready");
            
            String line;
            String sender = null;
            String recipient = null;
            StringBuilder emailData = new StringBuilder();
            boolean dataMode = false;
            
            while ((line = reader.readLine()) != null) {
                if (dataMode) {
                    if (".".equals(line.trim())) {
                        // End of email data
                        processEmailData(sender, recipient, emailData.toString());
                        writer.println("250 OK: Message accepted for delivery");
                        dataMode = false;
                        emailData.setLength(0);
                    } else {
                        emailData.append(line).append("\n");
                    }
                } else {
                    handleSMTPCommand(line.trim(), sender, recipient);
                    
                    if (line.startsWith("MAIL FROM:")) {
                        sender = extractEmail(line);
                        writer.println("250 OK");
                    } else if (line.startsWith("RCPT TO:")) {
                        recipient = extractEmail(line);
                        writer.println("250 OK");
                    } else if (line.startsWith("DATA")) {
                        writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        dataMode = true;
                    } else if (line.startsWith("QUIT")) {
                        writer.println("221 Bye");
                        break;
                    } else if (line.startsWith("HELO") || line.startsWith("EHLO")) {
                        writer.println("250 Hello");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling SMTP client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    private void handleSMTPCommand(String command, String sender, String recipient) {
        System.out.println("SMTP Command: " + command);
    }
    
    private String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return line.substring(start + 1, end);
        }
        return line.substring(line.indexOf(':') + 1).trim();
    }
    
    private void processEmailData(String sender, String recipient, String data) {
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
        email.setStatus(Email.EmailStatus.SENT);
        server.deliverEmail(email);
    }
}

// POP3 Server implementation
class POP3Server {
    private final int port;
    private final SMTPServer mailServer;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public POP3Server(int port, SMTPServer mailServer) {
        this.port = port;
        this.mailServer = mailServer;
        this.threadPool = Executors.newFixedThreadPool(5);
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
                    threadPool.submit(new POP3Handler(clientSocket, mailServer));
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
        } catch (IOException e) {
            System.err.println("Error stopping POP3 server: " + e.getMessage());
        }
    }
}

// POP3 Protocol Handler
class POP3Handler implements Runnable {
    private final Socket clientSocket;
    private final SMTPServer mailServer;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentUser;
    private boolean authenticated;
    
    public POP3Handler(Socket clientSocket, SMTPServer mailServer) {
        this.clientSocket = clientSocket;
        this.mailServer = mailServer;
        this.authenticated = false;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            writer.println("+OK POP3 server ready");
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                String command = parts[0].toUpperCase();
                
                switch (command) {
                    case "USER":
                        if (parts.length > 1) {
                            currentUser = parts[1];
                            writer.println("+OK User accepted");
                        } else {
                            writer.println("-ERR Invalid user command");
                        }
                        break;
                    case "PASS":
                        // Simple authentication (in real implementation, use proper auth)
                        authenticated = true;
                        writer.println("+OK Password accepted");
                        break;
                    case "STAT":
                        if (authenticated) {
                            List<Email> emails = mailServer.getEmails(currentUser);
                            writer.println("+OK " + emails.size() + " messages");
                        } else {
                            writer.println("-ERR Not authenticated");
                        }
                        break;
                    case "LIST":
                        if (authenticated) {
                            List<Email> emails = mailServer.getEmails(currentUser);
                            writer.println("+OK " + emails.size() + " messages");
                            for (int i = 0; i < emails.size(); i++) {
                                writer.println((i + 1) + " " + emails.get(i).toString().length());
                            }
                            writer.println(".");
                        } else {
                            writer.println("-ERR Not authenticated");
                        }
                        break;
                    case "RETR":
                        if (authenticated && parts.length > 1) {
                            try {
                                int msgNum = Integer.parseInt(parts[1]) - 1;
                                List<Email> emails = mailServer.getEmails(currentUser);
                                if (msgNum >= 0 && msgNum < emails.size()) {
                                    Email email = emails.get(msgNum);
                                    writer.println("+OK Message follows");
                                    writer.println(email.toString());
                                    writer.println(".");
                                    mailServer.markAsRead(currentUser, email.getMessageId());
                                } else {
                                    writer.println("-ERR No such message");
                                }
                            } catch (NumberFormatException e) {
                                writer.println("-ERR Invalid message number");
                            }
                        } else {
                            writer.println("-ERR Invalid command or not authenticated");
                        }
                        break;
                    case "QUIT":
                        writer.println("+OK Goodbye");
                        return;
                    default:
                        writer.println("-ERR Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling POP3 client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing POP3 client socket: " + e.getMessage());
            }
        }
    }
}

// Email Client with GUI
class EmailClient extends JFrame {
    private final SMTPServer mailServer;
    private final String currentUser;
    private DefaultTableModel inboxModel;
    private JTable inboxTable;
    private JTextArea emailContentArea;
    private JTextField toField, subjectField;
    private JTextArea bodyArea;
    
    public EmailClient(SMTPServer mailServer, String user) {
        this.mailServer = mailServer;
        this.currentUser = user;
        initializeGUI();
        refreshInbox();
    }
    
    private void initializeGUI() {
        setTitle("Email Client - " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Inbox tab
        JPanel inboxPanel = createInboxPanel();
        tabbedPane.addTab("Inbox", inboxPanel);
        
        // Compose tab
        JPanel composePanel = createComposePanel();
        tabbedPane.addTab("Compose", composePanel);
        
        add(tabbedPane);
        
        // Refresh inbox every 5 seconds
        Timer refreshTimer = new Timer(5000, e -> refreshInbox());
        refreshTimer.start();
    }
    
    private JPanel createInboxPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Email list
        String[] columns = {"From", "Subject", "Date", "Status"};
        inboxModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        inboxTable = new JTable(inboxModel);
        inboxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inboxTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedEmail();
            }
        });
        
        JScrollPane tableScrollPane = new JScrollPane(inboxTable);
        tableScrollPane.setPreferredSize(new Dimension(800, 200));
        
        // Email content area
        emailContentArea = new JTextArea();
        emailContentArea.setEditable(false);
        emailContentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane contentScrollPane = new JScrollPane(emailContentArea);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                              tableScrollPane, contentScrollPane);
        splitPane.setDividerLocation(200);
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshInbox());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createComposePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("To:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        toField = new JTextField();
        formPanel.add(toField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Subject:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        subjectField = new JTextField();
        formPanel.add(subjectField, gbc);
        
        // Body area
        bodyArea = new JTextArea(15, 40);
        bodyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane bodyScrollPane = new JScrollPane(bodyArea);
        bodyScrollPane.setBorder(BorderFactory.createTitledBorder("Message Body"));
        
        // Send button
        JButton sendButton = new JButton("Send Email");
        sendButton.addActionListener(this::sendEmail);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(bodyScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void refreshInbox() {
        SwingUtilities.invokeLater(() -> {
            inboxModel.setRowCount(0);
            List<Email> emails = mailServer.getEmails(currentUser);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            
            for (Email email : emails) {
                Object[] row = {
                    email.getSender(),
                    email.getSubject(),
                    sdf.format(email.getTimestamp()),
                    email.getStatus()
                };
                inboxModel.addRow(row);
            }
        });
    }
    
    private void displaySelectedEmail() {
        int selectedRow = inboxTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<Email> emails = mailServer.getEmails(currentUser);
            if (selectedRow < emails.size()) {
                Email email = emails.get(selectedRow);
                emailContentArea.setText(email.toString());
                mailServer.markAsRead(currentUser, email.getMessageId());
                refreshInbox();
            }
        }
    }
    
    private void sendEmail(ActionEvent e) {
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText();
        
        if (to.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter recipient email address.");
            return;
        }
        
        if (!isValidEmail(to)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
            return;
        }
        
        Email email = new Email(currentUser, to, subject, body);
        email.setStatus(Email.EmailStatus.SENT);
        mailServer.deliverEmail(email);
        
        // Clear form
        toField.setText("");
        subjectField.setText("");
        bodyArea.setText("");
        
        JOptionPane.showMessageDialog(this, "Email sent successfully!");
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
}

// Main application class
public class EmailSystemMain {
    public static void main(String[] args) {
        try {
            // Set look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
        
        // Start email servers
        SMTPServer smtpServer = new SMTPServer(2525);
        POP3Server pop3Server = new POP3Server(1100, smtpServer);
        
        // Start servers in separate threads
        Thread smtpThread = new Thread(() -> smtpServer.start());
        Thread pop3Thread = new Thread(() -> pop3Server.start());
        
        smtpThread.setDaemon(true);
        pop3Thread.setDaemon(true);
        
        smtpThread.start();
        pop3Thread.start();
        
        // Wait a moment for servers to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Launch email clients for demo
        SwingUtilities.invokeLater(() -> {
            // Create demo users
            EmailClient client1 = new EmailClient(smtpServer, "user1@example.com");
            EmailClient client2 = new EmailClient(smtpServer, "user2@example.com");
            
            client1.setVisible(true);
            client2.setLocation(client1.getX() + 50, client1.getY() + 50);
            client2.setVisible(true);
            
            // Add sample emails for demonstration
            addSampleEmails(smtpServer);
        });
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down email servers...");
            smtpServer.stop();
            pop3Server.stop();
        }));
    }
    
    private static void addSampleEmails(SMTPServer smtpServer) {
        // Add some sample emails for demonstration
        Email sample1 = new Email("admin@company.com", "user1@example.com", 
                                 "Welcome to Email System", 
                                 "Welcome to our advanced email system!\n\nThis is a sample email to demonstrate the functionality.");
        sample1.setStatus(Email.EmailStatus.DELIVERED);
        smtpServer.deliverEmail(sample1);
        
        Email sample2 = new Email("newsletter@tech.com", "user2@example.com", 
                                 "Weekly Tech Newsletter", 
                                 "Here are the latest updates in technology:\n\n1. Java 21 released\n2. New frameworks emerging\n3. AI advancements");
        sample2.setStatus(Email.EmailStatus.DELIVERED);
        smtpServer.deliverEmail(sample2);
    }
}
