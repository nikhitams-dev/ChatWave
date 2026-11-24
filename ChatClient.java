import java.io.*;
import java.net.*;

public class ChatClient {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionStatus(String status);
    }

    private MessageListener listener;

    public void connect(String username, MessageListener listener) {

        this.listener = listener;

        try {

            socket = new Socket("reseau.proxy.rlwy.net", 36371);

            input = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            output = new PrintWriter(
                socket.getOutputStream(), true
            );

            output.println(username);

            listener.onConnectionStatus("Connected");

            Thread receiveThread = new Thread(() -> {

                try {

                    String message;

                    while ((message = input.readLine()) != null) {

                        listener.onMessageReceived(message);
                    }

                } catch (IOException e) {

                    listener.onConnectionStatus("Disconnected");
                }
            });

            receiveThread.setDaemon(true);
            receiveThread.start();

        } catch (IOException e) {

            listener.onConnectionStatus("Connection failed");
            System.out.println("Unable to connect: " + e.getMessage());
        }
    }

    public synchronized void sendMessage(String message) {

        if (output != null && !socket.isClosed()) {
            output.println(message);
        }
    }

    public void disconnect() {

        try {

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (listener != null) {
                listener.onConnectionStatus("Disconnected");
            }

        } catch (IOException e) {

            System.out.println("Error disconnecting.");
        }
    }
}
