package flores.caro;

import flores.caro.utils.DBDAO;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private ServerSocket serverSocket;
    private int serverPort;
    private int threadPoolSize;
    private ExecutorService executor;
    private MessageProcessor messageProcessor;
    private DBDAO dao;
    private AtomicBoolean running = new AtomicBoolean(true);

    private void openServer() {
        loadProperties();
        dao = new DBDAO();
        messageProcessor = new MessageProcessor(dao);
        try {
            serverSocket = new ServerSocket(serverPort);
            executor = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("Server opened!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            properties.load(fis);
            serverPort = Integer.parseInt(properties.getProperty("serverPort"));
            threadPoolSize = Integer.parseInt(properties.getProperty("threadPoolSize"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void acceptClients() {
        try {
            while (running.get()) {
                Socket clientHandlerSocket = serverSocket.accept();
                System.out.println("Client accepted");

                ClientHandler clientHandler = new ClientHandler(clientHandlerSocket, messageProcessor);
                Thread clientHandlerThread = new Thread(clientHandler);
                executor.execute(clientHandlerThread);
                System.out.println("Client Handler Thread executed");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stop() {
        running.set(false);
    }

    static void main() {
        Server server = new Server();
        server.openServer();
        server.acceptClients();
    }
}
