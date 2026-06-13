package flores.caro;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private Server server;
    private Socket clientSocket;
    private MessageProcessor messageProcessor;
    private BufferedReader clientInput;
    private PrintWriter clientOutput;
    private Integer userId;

    public ClientHandler(Server server, Socket clientSocket, MessageProcessor messageProcessor) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.messageProcessor = messageProcessor;
    }

    public void sendMessage(String jsonMessage) {
        if (clientOutput != null)
            clientOutput.println(jsonMessage);
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public void run() {
        try {
            clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);

            String input;
            String response;
            // DataInputStream y DataOutputStream (?)
            while ((input = clientInput.readLine()) != null) {
                response = messageProcessor.processMessage(input, this);
                clientOutput.println(response);
            }

        } catch (SocketException e) {
            System.err.println("Socket exception - client socket closed");
        } catch (IOException e2) {
            System.err.println("IOException on ClientHandler thread: " + e2.getMessage());
        } finally {
            if (userId != null) {
                SessionManager.removeSession(userId);
                System.out.println("Removed user session from SessionManager");
            }

            if (server != null && clientSocket != null) {
                server.removeClientSocket(clientSocket);
            }

            try {
                if (clientInput != null)
                    clientInput.close();
                if (clientOutput != null)
                    clientOutput.close();
                if (clientSocket != null && !clientSocket.isClosed())
                    clientSocket.close();

                System.out.println("Client Handler socket closed");
            } catch (IOException e) {
                System.err.println("Error closing client socket and ClientHandler resources" + e.getMessage());
            }
        }
    }
}
