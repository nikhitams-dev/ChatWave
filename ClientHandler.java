import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ArrayList<ClientHandler> clients;

    private BufferedReader input;
    private PrintWriter output;
    private String username;

    private static AtomicLong messageCounter = new AtomicLong(1);

    public ClientHandler(Socket socket, ArrayList<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {

        try {

            input = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            output = new PrintWriter(socket.getOutputStream(), true);

            username = input.readLine();

            System.out.println(username + " joined ChatWave!");

            broadcast("SYSTEM|" + username + " joined the chat!");
            broadcastUserList();

            String message;

            while ((message = input.readLine()) != null) {

                if (message.startsWith("/typing|")) {

                    broadcastTyping(message.substring(8));
                    continue;
                }

                if (message.startsWith("/read|")) {

                    handleReadReceipt(message);
                    continue;
                }

                if (message.startsWith("/w ")) {

                    handlePrivateMessage(message);
                    continue;
                }

                handleGroupMessage(message);
            }

        } catch (IOException e) {

            System.out.println(username + " disconnected.");

        } finally {

            synchronized (clients) {
                clients.remove(this);
            }

            if (username != null) {
                broadcast("SYSTEM|" + username + " left the chat!");
                broadcastUserList();
            }

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection.");
            }
        }
    }

    private void handleGroupMessage(String message) {

        if (message.trim().isEmpty()) {
            return;
        }

        String id = String.valueOf(messageCounter.getAndIncrement());

        String formatted =
            "MSG|" + id + "|" + username + "|*|" + message;

        broadcast(formatted);
        send("DELIVERED|" + id);
    }

    private void handlePrivateMessage(String message) {

        String[] parts = message.split(" ", 3);

        if (parts.length < 3) {
            send("SYSTEM|Use /w username message");
            return;
        }

        String targetName = parts[1];
        String privateMessage = parts[2];

        String id = String.valueOf(messageCounter.getAndIncrement());

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.username != null &&
                    client.username.equalsIgnoreCase(targetName)) {

                    String formatted =
                        "MSG|" + id + "|" + username + "|" +
                        client.username + "|" + privateMessage;

                    client.send(formatted);
                    send(formatted);

                    send("DELIVERED|" + id);
                    return;
                }
            }
        }

        send("SYSTEM|User " + targetName + " is not online.");
    }

    private void handleReadReceipt(String message) {

        String[] parts = message.split("\\|", 3);

        if (parts.length < 3) {
            return;
        }

        String messageId = parts[1];
        String senderName = parts[2];

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.username != null &&
                    client.username.equalsIgnoreCase(senderName)) {

                    client.send("READ|" + messageId);
                    break;
                }
            }
        }
    }

    private void broadcastTyping(String status) {

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client != this && client.output != null) {

                    client.send(
                        "TYPING|" + username + "|" + status
                    );
                }
            }
        }
    }

    public void broadcast(String message) {

        synchronized (clients) {

            for (ClientHandler client : clients) {
                client.send(message);
            }
        }
    }

    public void broadcastUserList() {

        StringBuilder users = new StringBuilder("ONLINE|");

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.username != null) {

                    if (users.length() > 7) {
                        users.append(",");
                    }

                    users.append(client.username);
                }
            }

            for (ClientHandler client : clients) {
                client.send(users.toString());
            }
        }
    }

    private void send(String message) {

        if (output != null) {
            output.println(message);
        }
    }
}