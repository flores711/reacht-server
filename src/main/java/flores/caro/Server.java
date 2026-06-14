package flores.caro;

import flores.caro.utils.DBDAO;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private int serverPort;
    private int threadPoolSize;
    private ExecutorService executor;
    private MessageProcessor messageProcessor;
    private DBDAO dao;
    private Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());
    private AtomicBoolean running = new AtomicBoolean(true);

    // Relanzar las excepciones aqui y capturar en el main
    public Server() {
        loadProperties();
        dao = new DBDAO();
        messageProcessor = new MessageProcessor(dao);
        try {
            serverSocket = new ServerSocket(serverPort);
            executor = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("Server opened!");
        } catch (IOException e) {
            throw new RuntimeException("Could not open server. Port could be already taken", e);
        }
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            properties.load(fis);

            String port = properties.getProperty("serverPort");
            String poolSize = properties.getProperty("threadPoolSize");

            if (port == null || poolSize == null) {
                throw new IllegalArgumentException("Critical server error: required properties not found on server properties file. Could not open server.");
            }

            serverPort = Integer.parseInt(port);
            threadPoolSize = Integer.parseInt(poolSize);

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Critical server error: server properties file not found. Could not open server.");
        } catch (IOException e) {
            throw new RuntimeException("Critical server error: error reading server properties file. Could not open server.");
        } catch (NumberFormatException e) { // Del parseInt
            throw new RuntimeException("Critical server error: server properties do not have valid number format. Could not open server.");
        }
    }

    @Override
    public void run() {
        acceptClients();
    }

    private void acceptClients() {
        try {
            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Client accepted");

                ClientHandler clientHandler = new ClientHandler(this, clientSocket, messageProcessor);
                executor.execute(clientHandler);
                System.out.println("Client Handler Thread executed");
            }
        } catch (IOException e) {
            if (!running.get()) {
                System.out.println("Accept clients loop closed due to server shutdown");
            } else {
                System.err.println("Unexpected clients loop closed: " + e.getMessage());
            }
        }
    }

    public void removeClientSocket(Socket clientSocket) {
        clientSockets.remove(clientSocket);
    }

    private void stop() {
        // Para evitar que se ejecute una segunda vez con el shutdownHook después de hacer stop() controladamente desde terminal
        // Solo se ejecuta el método si running es true, si no hace return
        // Si es true, lo pone a false y devuelve true (no entra dentro del if por ! -> true)
        // Si es false, devuelve false y entra dentro del if por ! -> true
        if (!running.compareAndSet(true, false)) {
            return;
        }

        System.out.println("Shutting server down...");

        // Cerramos server socket para que no acepte más clientes
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing ServerSocket: " + e.getMessage());
        }

        // Para que no acepte nada más
        if (executor != null) {
            executor.shutdown();
        }

        // Cerramos los sockets de los clientes
        // Usamos synchronized para que ningun cliente pueda hacer remove de él mismo sobre este set
        // y de error por estar modificando el set a la vez que se está leyendo
        synchronized (clientSockets) {
            for (Socket socket : clientSockets) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close(); // Esto forzará una IOException en los hilos ClientHandler
                    }
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        // Si quedan aún hilos activos, esperamos 10 segundos máximo y forzamos cierre
        if (executor != null) {
            try {
                // Devuelve true si se terminó todo antes de tiempo y false si no
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Forcing shutdown of remaining active threads...");
                    executor.shutdownNow();
                }
            // Si se interrumpe al servidor esperando, terminamos de forzar
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();

            // Si se crea el objeto servidor (es decir, no ha habido ningún fallo en socket, loadProperties...
            // lanzamos el hilo para aceptar clientes
            // Así no bloqueamos el hilo prinicipal y podemos hacer funcionalidad de leer teclado,
            // para poder cerrar el servidor manualmente escribiendo "S" por terminal
            Thread serverThread = new Thread(server);
            serverThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
            }));

            Scanner sc = new Scanner(System.in);
            System.out.println("===== Write 'S' and press Enter to Stop server =====");

            while (true) {
                String input = sc.next();
                if (input.equalsIgnoreCase("S")) {
                    break;
                }
            }

            // Si ha escrito "S", llamamos a stop y esperamos a que el hilo termine también
            server.stop();
            serverThread.join();

            System.out.println("Server stopped successfully by user");

        // Capturar aqui excepciones y mostrar mensaje por pantalla
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
