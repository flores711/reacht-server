package flores.caro;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientHandlerSocket;
    private MessageProcessor messageProcessor;

    public ClientHandler(Socket clientHandlerSocket, MessageProcessor messageProcessor) {
        this.clientHandlerSocket = clientHandlerSocket;
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void run() {
        try (BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientHandlerSocket.getInputStream()));
            PrintWriter clientOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientHandlerSocket.getOutputStream())), true)) {

            String input;
            String response;
            while ((input = clientInput.readLine()) != null) {
                response = messageProcessor.processMessage(input);
                clientOutput.println(response);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (clientHandlerSocket != null)
                    clientHandlerSocket.close();
                System.out.println("Client Handler socket closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
