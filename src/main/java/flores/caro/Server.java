package flores.caro;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// Crear clases de entidades con hibernate incorporado para la BD
// DAO
// Protocolo


public class Server {
    private ServerSocket serverSocket;
    private int serverPort;
    private ExecutorService executor;

    private void openServer() {
        loadProperties();
        try {
            serverSocket = new ServerSocket(serverPort);
            // TODO: Change to obtained from properties
            executor = Executors.newFixedThreadPool(20);

            System.out.println("Server opened!");
        } catch (IOException e) {
            throw new RuntimeException(e);
            // TODO
        }
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            properties.load(fis);
            serverPort = Integer.parseInt(properties.getProperty("serverPort"));
        } catch (IOException e) {
            throw new RuntimeException(e);
            // TODO
        }
    }

    private void acceptClients() {
        try {
            while (true) {
                Socket clientHandlerSocket = serverSocket.accept();
                System.out.println("Client accepted");

                ClientHandler clientHandler = new ClientHandler(clientHandlerSocket);
                Thread clientHandlerThread = new Thread(clientHandler);
                executor.execute(clientHandlerThread);
                System.out.println("Client Handler Thread executed");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
            // TODO
        }
    }

    static void main() {
        Server server = new Server();
        server.openServer();
        server.acceptClients();
    }
}
