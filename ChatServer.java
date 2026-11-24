import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ChatServer {

    public static final int PORT = 5000;
    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("Starting ChatWave Server...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("ChatWave Server started on port " + PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(socket, clients);

                synchronized (clients) {
                    clients.add(handler);
                }

                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}