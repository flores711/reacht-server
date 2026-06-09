package flores.caro;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientHandlerSocket;
    private MessageProcessor messageProcessor;
    private BufferedReader clientInput;
    private PrintWriter clientOutput;
    private Integer userId;

    public ClientHandler(Socket clientHandlerSocket, MessageProcessor messageProcessor) {
        this.clientHandlerSocket = clientHandlerSocket;
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
            clientInput = new BufferedReader(new InputStreamReader(clientHandlerSocket.getInputStream()));
            clientOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientHandlerSocket.getOutputStream())), true);

            String input;
            String response;
            // DataInputStream y DataOutputStream (?)
            while ((input = clientInput.readLine()) != null) {
                response = messageProcessor.processMessage(input, this);
                clientOutput.println(response);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (userId != null) {
                SessionManager.removeSession(userId);
            }

            try {
                if (clientHandlerSocket != null)
                    clientHandlerSocket.close();
                System.out.println("Client Handler socket closed");
                clientInput.close();
                clientOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public BufferedReader getClientInput() {
        return clientInput;
    }

    public PrintWriter getClientOutput() {
        return clientOutput;
    }
}
