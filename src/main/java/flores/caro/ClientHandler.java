package flores.caro;

import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientHandlerSocket;

    public ClientHandler(Socket clientHandlerSocket) {
        this.clientHandlerSocket = clientHandlerSocket;
    }


    @Override
    public void run() {
        logIn();
    }

    private void logIn() {
        // Comprobar credenciales almacenadas
        // Si no, se muestra la primera pantalla de iniciar sesión
        //      Cuando se registre / inicie sesión, almacenar credenciales
        // Si sí se encuentran credenciales, iniciar sesión con ellas y acceder a app directamente
        // Esto es el servidor, así que entiendo que el cliente también me debe mandar estos datos y yo los compruebo con la BD

        // (Mirar cómo se hace en apps reales, si es así o de otra forma)

        // Y dónde pongo todo el resto de acciones? Por ej buscar oferta, crear una
        // En esta clase? O hago clase aparte con todo?
        // --> DAO?
        // Pero igualmente aparte del DAO, dónde manejo cada acción
        // --> Claro, entiendo que eso se maneja desde la interfaz
        //     La interfaz mandará X acciones y esto lo manejará
        //     --> En el propio run(), o en otro sitio?
        //         O que el run() llame a otra función de manejar acción y dependiendo de lo que envíe el cliente, llamar a una función u otra
        //         --> Supongo que mirar igualmente también cómo se hace en apps reales

        // Mirar Gemini Pro con cuenta instituto
    }
}
