package flores.caro;

import java.util.concurrent.ConcurrentHashMap;

// Mapa de UserId en base de datos con su ClientHandler en el servidor
// Guardamos id para no guardar usuarios desactualizados
// Usado principalmente para el broadcast de mensajes de chat
public class SessionManager {
    private static final ConcurrentHashMap<Integer, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    public static void addSession(Integer userId, ClientHandler handler) {
        if (userId != null && handler != null) {
            activeSessions.put(userId, handler);
            System.out.println("User " + userId + " connected and registered in net");
        }
    }

    public static void removeSession(Integer userId) {
        if (userId != null) {
            activeSessions.remove(userId);
            System.out.println("User " + userId + " disconnected and deleted from net");
        }
    }

    public static ClientHandler getSession(Integer userId) {
        return activeSessions.get(userId);
    }
}