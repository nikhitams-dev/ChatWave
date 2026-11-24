import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGUI extends JFrame {

    private ChatClient client;

    private JPanel messagesPanel;
    private JPanel usersPanel;
    private JScrollPane messagesScroll;

    private JTextField messageField;

    private JLabel chatTitle;
    private JLabel statusLabel;
    private JLabel typingLabel;

    private String username;
    private String selectedUser = null;

    private ConcurrentHashMap<String, JLabel> messageStatuses =
        new ConcurrentHashMap<>();

    // Stores private messages that have arrived but are not yet read.
    private ConcurrentHashMap<String, Set<String>> pendingReadReceipts =
        new ConcurrentHashMap<>();

    private Color backgroundColor = new Color(18, 20, 38);
    private Color panelColor = new Color(29, 32, 55);
    private Color accentColor = new Color(130, 100, 255);
    private Color receivedColor = new Color(48, 52, 80);
    private Color sentColor = new Color(100, 75, 200);

    private Font getReceiptFont() {
        return new Font("Segoe UI Symbol", Font.PLAIN, 12);
    }

    public ChatGUI() {

        setTitle("ChatWave - Connect and Chat");
        setSize(1150, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        getContentPane().setBackground(backgroundColor);
        setLayout(new BorderLayout());

        createSidebar();
        createChatArea();
        createUsersPanel();

        setVisible(true);

        username = JOptionPane.showInputDialog(
            this,
            "Enter your username:",
            "Welcome to ChatWave",
            JOptionPane.PLAIN_MESSAGE
        );

        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        username = username.trim();

        client = new ChatClient();

        client.connect(username, new ChatClient.MessageListener() {

            @Override
            public void onMessageReceived(String message) {
                SwingUtilities.invokeLater(() ->
                    handleServerMessage(message)
                );
            }

            @Override
            public void onConnectionStatus(String status) {
                SwingUtilities.invokeLater(() ->
                    statusLabel.setText(status)
                );
            }
        });

        setupTypingListener();
    }

    private void createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setBackground(panelColor);
        sidebar.setPreferredSize(new Dimension(210, 700));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(30, 20, 20, 20));

        JLabel logo = new JLabel("ChatWave");
        logo.setFont(new Font("Arial", Font.BOLD, 27));
        logo.setForeground(accentColor);

        JLabel tagline = new JLabel("Connect. Chat. Share.");
        tagline.setForeground(Color.LIGHT_GRAY);

        JButton globalButton = new JButton("Global Chat");
        styleButton(globalButton, accentColor);
        globalButton.setMaximumSize(new Dimension(170, 42));
        globalButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        globalButton.addActionListener(e -> {
            selectedUser = null;
            chatTitle.setText("Global Chat");
            messageField.setToolTipText("Type a group message...");
            typingLabel.setText(" ");
        });

        JButton clearButton = new JButton("Clear Chat");
        styleButton(clearButton, receivedColor);
        clearButton.setMaximumSize(new Dimension(170, 42));
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        clearButton.addActionListener(e -> {
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
            messageStatuses.clear();
        });

        JButton logoutButton = new JButton("Logout");
        styleButton(logoutButton, new Color(180, 65, 85));
        logoutButton.setMaximumSize(new Dimension(170, 42));
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        logoutButton.addActionListener(e -> logout());

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(45));
        sidebar.add(globalButton);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(clearButton);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);
        sidebar.add(Box.createVerticalStrut(20));

        JLabel footer = new JLabel("CHATWAVE v3.0");
        footer.setForeground(Color.GRAY);
        sidebar.add(footer);

        add(sidebar, BorderLayout.WEST);
    }

    private void createChatArea() {

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(backgroundColor);

        chatTitle = new JLabel("Global Chat");
        chatTitle.setFont(new Font("Arial", Font.BOLD, 25));
        chatTitle.setForeground(Color.WHITE);

        statusLabel = new JLabel("Connecting...");
        statusLabel.setForeground(new Color(80, 220, 140));

        typingLabel = new JLabel(" ");
        typingLabel.setForeground(new Color(180, 160, 255));
        typingLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        JPanel headerText = new JPanel();
        headerText.setBackground(backgroundColor);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        headerText.add(chatTitle);
        headerText.add(Box.createVerticalStrut(7));
        headerText.add(statusLabel);
        headerText.add(Box.createVerticalStrut(5));
        headerText.add(typingLabel);

        header.add(headerText, BorderLayout.WEST);

        messagesPanel = new JPanel();
        messagesPanel.setBackground(backgroundColor);
        messagesPanel.setLayout(
            new BoxLayout(messagesPanel, BoxLayout.Y_AXIS)
        );
        messagesPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.setBorder(null);
        messagesScroll.getViewport().setBackground(backgroundColor);
        messagesScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(backgroundColor);

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 15));
        messageField.setBackground(panelColor);
        messageField.setForeground(Color.WHITE);
        messageField.setCaretColor(Color.WHITE);
        messageField.setBorder(new EmptyBorder(15, 15, 15, 15));
        messageField.setToolTipText("Type a message...");

        JButton sendButton = new JButton("SEND");
        styleButton(sendButton, accentColor);
        sendButton.setFont(new Font("Arial", Font.BOLD, 13));

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(messagesScroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void createUsersPanel() {

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(panelColor);
        rightPanel.setPreferredSize(new Dimension(220, 700));
        rightPanel.setBorder(new EmptyBorder(25, 15, 20, 15));

        JLabel heading = new JLabel("ONLINE USERS");
        heading.setFont(new Font("Arial", Font.BOLD, 15));
        heading.setForeground(Color.WHITE);

        JLabel instruction = new JLabel("Click a name to chat");
        instruction.setForeground(Color.LIGHT_GRAY);

        JPanel top = new JPanel();
        top.setBackground(panelColor);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(heading);
        top.add(Box.createVerticalStrut(8));
        top.add(instruction);

        usersPanel = new JPanel();
        usersPanel.setBackground(panelColor);
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));

        JScrollPane usersScroll = new JScrollPane(usersPanel);
        usersScroll.setBorder(null);
        usersScroll.getViewport().setBackground(panelColor);

        rightPanel.add(top, BorderLayout.NORTH);
        rightPanel.add(usersScroll, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);
    }

    private void styleButton(JButton button, Color color) {

        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void handleServerMessage(String message) {

        if (message.startsWith("ONLINE|")) {

            updateOnlineUsers(message.substring(7));

        } else if (message.startsWith("TYPING|")) {

            String[] parts = message.split("\\|", 3);

            if (parts.length == 3) {
                typingLabel.setText(
                    parts[2].equals("typing")
                    ? parts[1] + " is typing..."
                    : " "
                );
            }

        } else if (message.startsWith("MSG|")) {

            handleChatMessage(message);

        } else if (message.startsWith("DELIVERED|")) {

            String id = message.substring(10);
            updateMessageStatus(id, "Delivered \u2713\u2713", false);

        } else if (message.startsWith("READ|")) {

            String id = message.substring(5);
            updateMessageStatus(id, "Read \u2713\u2713", true);

        } else if (message.startsWith("SYSTEM|")) {

            addMessage(message.substring(7), false, null);

        } else {

            addMessage(message, false, null);
        }
    }

    private void handleChatMessage(String message) {

        String[] parts = message.split("\\|", 5);

        if (parts.length < 5) {
            return;
        }

        String id = parts[1];
        String sender = parts[2];
        String recipient = parts[3];
        String content = parts[4];

        boolean sentByMe = sender.equalsIgnoreCase(username);

        String displayMessage;

        if (recipient.equals("*")) {

            displayMessage = sender + ": " + content;

        } else if (sentByMe) {

            displayMessage = "You (private to " + recipient + "): " + content;

        } else {

            displayMessage = sender + " (private): " + content;
        }

        addMessage(displayMessage, sentByMe, id);

        // Mark a private message as read only if its chat is open.
        if (!sentByMe && !recipient.equals("*")) {

            if (selectedUser != null &&
                selectedUser.equalsIgnoreCase(sender)) {

                sendReadReceipt(id, sender);

            } else {

                pendingReadReceipts
                    .computeIfAbsent(sender.toLowerCase(),
                        key -> ConcurrentHashMap.newKeySet())
                    .add(id);
            }
        }
    }

    // Sends read receipts for messages from the selected conversation.
    private void markPendingMessagesAsRead(String sender) {

        Set<String> pending = pendingReadReceipts.remove(
            sender.toLowerCase()
        );

        if (pending != null) {

            for (String id : pending) {
                sendReadReceipt(id, sender);
            }
        }
    }

    private void sendReadReceipt(String id, String sender) {

        if (client != null) {
            client.sendMessage("/read|" + id + "|" + sender);
        }
    }

    private void updateOnlineUsers(String userList) {

        usersPanel.removeAll();

        if (!userList.trim().isEmpty()) {

            String[] users = userList.split(",");

            for (String user : users) {

                String name = user.trim();

                if (name.isEmpty() ||
                    name.equalsIgnoreCase(username)) {
                    continue;
                }

                JButton userButton = new JButton("\u25CF  " + name);

                userButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                userButton.setMaximumSize(new Dimension(190, 40));
                userButton.setBackground(panelColor);
                userButton.setForeground(new Color(90, 230, 150));
                userButton.setFont(new Font("Arial", Font.BOLD, 13));
                userButton.setFocusPainted(false);
                userButton.setBorderPainted(false);
                userButton.setHorizontalAlignment(SwingConstants.LEFT);

                userButton.addActionListener(e -> {

                    selectedUser = name;
                    chatTitle.setText("Private Chat with " + name);
                    messageField.setToolTipText("Message to " + name);
                    typingLabel.setText(" ");

                    // Opening this chat marks its pending messages as read.
                    markPendingMessagesAsRead(name);
                });

                usersPanel.add(userButton);
                usersPanel.add(Box.createVerticalStrut(8));
            }
        }

        usersPanel.revalidate();
        usersPanel.repaint();
    }

    private void setupTypingListener() {

        messageField.getDocument().addDocumentListener(
            new DocumentListener() {

                private void notifyTyping() {

                    if (client == null) {
                        return;
                    }

                    if (messageField.getText().trim().isEmpty()) {
                        client.sendMessage("/typing|stopped");
                    } else {
                        client.sendMessage("/typing|typing");
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    notifyTyping();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    notifyTyping();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    notifyTyping();
                }
            }
        );
    }

    private void addMessage(
        String message,
        boolean sentByMe,
        String messageId
    ) {

        JPanel row = new JPanel(new FlowLayout(
            sentByMe ? FlowLayout.RIGHT : FlowLayout.LEFT
        ));

        row.setBackground(backgroundColor);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(sentByMe ? sentColor : receivedColor);
        bubble.setBorder(new EmptyBorder(12, 15, 8, 15));

        JLabel messageLabel = new JLabel(
            "<html><div style='width:240px;'>" +
            escapeHtml(message) +
            "</div></html>"
        );

        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        String time = LocalTime.now().format(
            DateTimeFormatter.ofPattern("hh:mm a")
        );

        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(new Color(210, 210, 225));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        footer.setBackground(sentByMe ? sentColor : receivedColor);

        footer.add(timeLabel);

        if (sentByMe && messageId != null) {

            JLabel receiptLabel = new JLabel("Sent \u2713");
            receiptLabel.setForeground(new Color(210, 210, 225));
            receiptLabel.setFont(getReceiptFont());

            footer.add(receiptLabel);
            messageStatuses.put(messageId, receiptLabel);
        }

        bubble.add(messageLabel);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(footer);

        row.add(bubble);

        messagesPanel.add(row);
        messagesPanel.add(Box.createVerticalStrut(8));

        messagesPanel.revalidate();
        messagesPanel.repaint();

        SwingUtilities.invokeLater(() ->
            messagesScroll.getVerticalScrollBar().setValue(
                messagesScroll.getVerticalScrollBar().getMaximum()
            )
        );
    }

    private void updateMessageStatus(
        String id,
        String status,
        boolean isRead
    ) {

        JLabel label = messageStatuses.get(id);

        if (label != null) {

            label.setText(status);
            label.setFont(getReceiptFont());

            if (isRead) {
                label.setForeground(new Color(90, 190, 255));
            } else {
                label.setForeground(new Color(210, 210, 225));
            }

            label.revalidate();
            label.repaint();
        }
    }

    private String escapeHtml(String text) {

        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private void sendMessage() {

        String message = messageField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        if (client != null) {

            if (selectedUser != null) {

                client.sendMessage(
                    "/w " + selectedUser + " " + message
                );

            } else {

                client.sendMessage(message);
            }
        }

        messageField.setText("");
    }

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Logout",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {

            if (client != null) {
                client.disconnect();
            }

            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new ChatGUI());
    }
}