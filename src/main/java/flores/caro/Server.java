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

    // TODO: HACER FUNCIÓN PODER CERRAR SERVIDOR CONTROLADAMENTE
    public Server() {
        loadProperties();
        dao = new DBDAO();
        messageProcessor = new MessageProcessor(dao);
        try {
            serverSocket = new ServerSocket(serverPort);
            executor = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("Server opened!");
        } catch (IOException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    // TODO: Qué pasa si no se cargan las properties bien, si no están en el formato correcto...
    // No se debe construir siquiera el objeto servidor si eso ocurre
    private void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            properties.load(fis);
            serverPort = Integer.parseInt(properties.getProperty("serverPort"));
            threadPoolSize = Integer.parseInt(properties.getProperty("threadPoolSize"));
        } catch (IOException e) {
            // TODO
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
            // TODO
            throw new RuntimeException(e);
        }
    }

    private void stop() {
        running.set(false);
    }

    static void main() {
        Server server = new Server();
        server.acceptClients();
    }
}
