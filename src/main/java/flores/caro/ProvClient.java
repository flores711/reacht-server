package flores.caro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ProvClient {
    private final static String IP_SERVIDOR = "localhost";
    private final static int PUERTO_SERVIDOR = 4444;
    private static int id;

    static void main() {
        try (Socket socketCliente = new Socket(IP_SERVIDOR, PUERTO_SERVIDOR);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
             PrintWriter salida = new PrintWriter(socketCliente.getOutputStream(), true);
             Scanner sc = new Scanner(System.in)
        ) {
            Thread hiloEscucha = new Thread(() -> {
                try {
                    String mensajePartida;
                    while ((mensajePartida = entrada.readLine()) != null) {
                        System.out.println(mensajePartida);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            hiloEscucha.setDaemon(true);
            hiloEscucha.start();

            while (true) {
                String entradaTecladoCliente = sc.nextLine();
                salida.println(entradaTecladoCliente);

                if (entradaTecladoCliente.equalsIgnoreCase("Salir"))
                    break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
