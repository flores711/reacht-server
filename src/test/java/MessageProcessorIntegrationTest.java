import flores.caro.ClientHandler;
import flores.caro.MessageProcessor;
import flores.caro.model.DataPackage;
import flores.caro.model.entities.Offer;
import flores.caro.model.entities.User;
import flores.caro.model.entities.Videogame;
import flores.caro.utils.DBDAO;
import flores.caro.utils.HibernateUtil;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/// TESTS ANTIGUOS DESACTUALIZADOS

public class MessageProcessorIntegrationTest {

    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        messageProcessor = new MessageProcessor(new DBDAO());

        // Vaciar todo de las tablas antes de cada test
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();

            // Desactivamos restricciones de claves foráneas temporalmente para poder vaciar todo
            session.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            session.createNativeQuery("TRUNCATE TABLE message").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE offer").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE chat").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE user").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE user_chat").executeUpdate();

            session.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

            session.getTransaction().commit();
        }
    }

    // Metodo auxiliar ara crear usuarios cuando un test lo requiera
    private User createTestUser(String username, String email, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            session.persist(user);
            session.getTransaction().commit();
            return user;
        }
    }

    // ==========================================
    // STRUCTURE AND ERROR TESTS
    // ==========================================

    @Test
    void unknownActionTest() {
        String requestJson = "{\"action\":\"DELETE_ACCOUNT\",\"data\":{\"username\":\"testuser\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Unknown action requested", response.getData().get("message"));
    }

    @Test
    void malformedJsonTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\"}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Malformed JSON or deserialization error", response.getData().get("message"));
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void loginSuccessTest() {
        createTestUser("testuser", "testuser@example.com", "password123");

        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"password123\"}}";

        // Simulamos en este test un clientHandler porque la función logIn() necesita hacer acciones con él, no puede ser null
        // Puesto -Dnet.bytebuddy.experimental=true en VM options en Edit Configurations al ejecutar porque bytebuddy oficial no soporta aún java 25
        ClientHandler mockHandler = org.mockito.Mockito.mock(ClientHandler.class);

        String responseJson = messageProcessor.processMessage(requestJson, mockHandler);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_SUCCESS", response.getAction());
        assertEquals("testuser", response.getData().get("username"));
        assertEquals("NORMAL", response.getData().get("role"));
    }

    @Test
    void loginWrongPasswordTest() {
        createTestUser("testuser", "testuser@example.com", "password123");

        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"wrong_password\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Wrong password for this username", response.getData().get("message"));
    }

    @Test
    void loginUserDoesNotExistTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"fakeuser999\",\"password\":\"password123\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Username doesn't exist", response.getData().get("message"));
    }

    // ==========================================
    // SIGN UP TESTS
    // ==========================================

    @Test
    void signUpSuccessTest() {
        String requestJson = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"newuser@example.com\",\"username\":\"newuser\",\"password\":\"newpassword123\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_SUCCESS", response.getAction());
        assertEquals("Account created successfully", response.getData().get("message"));
    }

    @Test
    void signUpEmailAlreadyExistsTest() {
        // Registramos al primer usuario
        String requestJson1 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"testuser@example.com\",\"username\":\"user_one\",\"password\":\"password123\"}}";
        messageProcessor.processMessage(requestJson1, null);

        // Intentamos registrar a otro usuario con distinto username pero el mismo EMAIL
        String requestJson2 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"testuser@example.com\",\"username\":\"user_two\",\"password\":\"password123\"}}";
        String responseJson = messageProcessor.processMessage(requestJson2, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_ERROR", response.getAction());
        assertEquals("Email is already registered", response.getData().get("message"));
    }

    @Test
    void signUpUsernameAlreadyExistsTest() {
        // Registramos al primer usuario
        String requestJson1 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"first@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}}";
        messageProcessor.processMessage(requestJson1, null);

        // Intentamos registrar a otro usuario con distinto email pero el mismo USERNAME
        String requestJson2 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"second@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}}";
        String responseJson = messageProcessor.processMessage(requestJson2, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_ERROR", response.getAction());
        assertEquals("Username is already taken", response.getData().get("message"));
    }

    // ==========================================
    // CREATE OFFER TESTS
    // ==========================================

    @Test
    void createOfferSuccessTest() {
        // Creamos el usuario que va a publicar la oferta
        User creator = createTestUser("offercreator", "creator@test.com", "1234");

        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Chill game\",\"creator\":{\"id\":" + creator.getId() + "}}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_SUCCESS", response.getAction());
        assertEquals("Offer and Chat created successfully", response.getData().get("message"));
    }

    @Test
    void createOfferUserAlreadyHasActiveOfferTest() {
        User creator = createTestUser("offercreator", "creator@test.com", "1234");
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Chill game\",\"creator\":{\"id\":" + creator.getId() + "}}}";

        // Primer intento: crea la oferta con éxito
        messageProcessor.processMessage(requestJson, null);

        // Segundo intento: debe fallar porque ya tiene una activa
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("Could not create offer. User already has an active one", response.getData().get("message"));
    }

    @Test
    void createOfferUserNotFoundTest() {
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Ghost game\",\"creator\":{\"id\":999999}}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("The user does not exist in the database", response.getData().get("message"));
    }

    @Test
    void createOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Bad game\",\"creator\":\"This is not an ID object\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for offer", response.getData().get("message"));
    }

    // ==========================================
    // DELETE OFFER TESTS
    // ==========================================

    @Test
    void deleteOfferSuccessTest() {
        User creator = createTestUser("deleter", "deleter@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Delete test offer\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson, null);

        // Coger id de la oferta creada de la BD (porque es automática)
        int offerId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":" + offerId + "}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_SUCCESS", response.getAction());
        assertEquals("Offer deleted successfully", response.getData().get("message"));
    }

    @Test
    void deleteOfferNotFoundTest() {
        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":999999}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_ERROR", response.getAction());
        assertEquals("Error deleting the offer", response.getData().get("message"));
    }

    @Test
    void deleteOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":\"not a number\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for delete offer", response.getData().get("message"));
    }

    // ==========================================
    // JOIN OFFER TESTS
    // ==========================================

    @Test
    void joinOfferSuccessTest() {
        // Creamos el creador y su oferta
        User creator = createTestUser("creator_join", "creator_join@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Join me\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson, null);

        // Cogemos el ID de la oferta creada
        int offerId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Usuario que se va a unir
        User joiner = createTestUser("joiner", "joiner@test.com", "1234");

        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + joiner.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_SUCCESS", response.getAction());
        assertEquals("Joined offer successfully", response.getData().get("message"));
    }

    @Test
    void joinOfferOfferNotFoundTest() {
        User joiner = createTestUser("joiner", "joiner@test.com", "1234");

        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":999999,\"user_id\":" + joiner.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("The offer no longer exists", response.getData().get("message"));
    }

    @Test
    void joinOfferUserNotFoundTest() {
        // Creamos el creador y su oferta
        User creator = createTestUser("creator_join", "creator_join@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Join me\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson, null);

        int offerId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Intentamos unirnos con usuario que no existe
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":999999}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("The user does not exist in the database", response.getData().get("message"));
    }

    @Test
    void joinOfferUserAlreadyInAnOfferTest() {
        // Oferta A
        User creatorA = createTestUser("creatorA", "creatorA@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Offer A\",\"creator\":{\"id\":" + creatorA.getId() + "}}}", null);

        int offerIdA;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creatorA.getId());
            offerIdA = u.getCurrentOffer().getId();
        }

        // Usuario B y oferta B (y usuario B ya está en una oferta activa)
        User creatorB = createTestUser("creatorB", "creatorB@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Offer B\",\"creator\":{\"id\":" + creatorB.getId() + "}}}", null);

        // Usuario B intenta unirse a la oferta A
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerIdA + ",\"user_id\":" + creatorB.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("Could not join offer. User is already in an active offer", response.getData().get("message"));
    }

    @Test
    void joinOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":\"invalid\",\"user_id\":\"invalid\"}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for join offer", response.getData().get("message"));
    }

    // ==========================================
    // LEAVE OFFER TESTS
    // ==========================================

    @Test
    void leaveOfferSuccessTest() {
        // Creador hace la oferta
        User creator = createTestUser("creator_leave", "creator_leave@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Leave me\",\"creator\":{\"id\":" + creator.getId() + "}}}", null);

        int offerId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Jugador se une a la oferta
        User leaver = createTestUser("leaver", "leaver@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + leaver.getId() + "}}", null);

        // Jugador abandona la oferta
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + leaver.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_SUCCESS", response.getAction());
        assertEquals("Left the offer successfully", response.getData().get("message"));
    }

    @Test
    void leaveOfferErrorNotAMemberTest() {
        // Creador hace la oferta
        User creator = createTestUser("creator_leave2", "creator_leave2@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Leave me\",\"creator\":{\"id\":" + creator.getId() + "}}}", null);

        int offerId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Creamos un usuario pero NO lo unimos a la oferta
        User stranger = createTestUser("stranger", "stranger@test.com", "1234");

        // El usuario intenta abandonar una oferta en la que no está
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + stranger.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_ERROR", response.getAction());
        assertEquals("Error trying to leave the offer", response.getData().get("message"));
    }

    @Test
    void leaveOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":\"invalid\",\"user_id\":\"invalid\"}}";
        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for leave offer", response.getData().get("message"));
    }

    // ==========================================
    // SEND MESSAGE TESTS (CHAT)
    // ==========================================

    @Test
    void sendMessageSuccessTest() {
        // Creamos al creador y publicamos la oferta, se genera un Chat en la BD
        User creator = createTestUser("chat_creator", "chat_creator@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Chatting game\",\"creator\":{\"id\":" + creator.getId() + "}}}", null);

        // Obtenemos el Chat asociado a la oferta que se acaba de crear
        int chatId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            chatId = u.getCurrentOffer().getChat().getId();
        }

        User joiner = createTestUser("chat_joiner", "chat_joiner@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + chatId + ",\"user_id\":" + joiner.getId() + "}}", null);

        String requestJson = "{\"action\":\"SEND_MESSAGE\",\"data\":{\"text\":\"Hello party!\",\"user\":{\"id\":" + creator.getId() + "},\"chat\":{\"id\":" + chatId + "}}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SEND_MESSAGE_SUCCESS", response.getAction());
        assertEquals("Message broadcasted to chat successfully", response.getData().get("message"));
    }

    @Test
    void sendMessageInvalidFormatTest() {
        // Le pasamos Strings en lugar de objetos JSON para el usuario y el chat
        String requestJson = "{\"action\":\"SEND_MESSAGE\",\"data\":{\"text\":\"Falla!\",\"user\":\"esto no es un objeto\",\"chat\":\"tampoco\"}}";

        String responseJson = messageProcessor.processMessage(requestJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SEND_MESSAGE_ERROR", response.getAction());
        assertEquals("Invalid data format received for message", response.getData().get("message"));
    }

    // ==========================================
    // GET CHAT HISTORY TESTS
    // ==========================================

    @Test
    void getChatHistorySuccessTest() {
        User creator = createTestUser("history_creator", "hist@example.com", "1234");

        // Creamos una oferta que crea un chat
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Test history\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson, null);

        // Extraemos el chatId de la BD
        int chatId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            chatId = u.getCurrentOffer().getChat().getId();
        }

        // El creador envía un mensaje a ese chat
        String sendMsgJson = "{\"action\":\"SEND_MESSAGE\",\"data\":{\"text\":\"First message!\",\"user\":{\"id\":" + creator.getId() + "},\"chat\":{\"id\":" + chatId + "}}}";
        messageProcessor.processMessage(sendMsgJson, null);

        // Solicitamos el historial
        String historyJson = "{\"action\":\"GET_CHAT_HISTORY\",\"data\":{\"chat_id\":" + chatId + "}}";
        String responseJson = messageProcessor.processMessage(historyJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("GET_CHAT_HISTORY_SUCCESS", response.getAction());

        List<Map<String, Object>> messages = (List<Map<String, Object>>) response.getData().get("messages");

        assertFalse(messages.isEmpty(), "Message list shouldn't be empty");
        assertEquals("First message!", messages.getFirst().get("text"));
        assertEquals("history_creator", messages.getFirst().get("user_username"));
    }

    @Test
    void getChatHistoryInvalidFormatTest() {
        // Solicitamos el historial mandando un texto en lugar de un número en chat_id
        String historyJson = "{\"action\":\"GET_CHAT_HISTORY\",\"data\":{\"chat_id\":\"esto_deberia_ser_un_numero\"}}";
        String responseJson = messageProcessor.processMessage(historyJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("GET_CHAT_HISTORY_ERROR", response.getAction());
        assertEquals("Invalid format requested for chat history", response.getData().get("message"));
    }

    // ==========================================
    // LEAVE CHAT TESTS
    // ==========================================

    @Test
    void leaveChatSuccessTest() {
        // Creamos un usuario, una oferta y un chat
        User creator = createTestUser("leave_creator", "leave1@example.com", "1234");

        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Test leave chat\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson, null);

        // Extraemos el chatId de la BD
        int chatId;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            chatId = u.getCurrentOffer().getChat().getId();
        }

        // Usuario abandona el chat
        String leaveChatJson = "{\"action\":\"LEAVE_CHAT\",\"data\":{\"chat_id\":" + chatId + ", \"user_id\":" + creator.getId() + "}}";
        String responseJson = messageProcessor.processMessage(leaveChatJson, null);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_CHAT_SUCCESS", response.getAction());
        assertEquals("Left the chat successfully", response.getData().get("message")); // Cámbialo si tu server devuelve otro mensaje exacto
    }







    // ==========================================
    // SEARCH OFFERS TESTS
    // ==========================================

    // Método auxiliar para obtener un videojuego que YA existe en la BD
    private Videogame getExistingVideogame(String title) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Videogame v WHERE v.title = :title", Videogame.class)
                    .setParameter("title", title)
                    .uniqueResult();
        }
    }

    // Método auxiliar para crear ofertas asociándolas a datos reales
    private Offer createTestOffer(User creator, Videogame videogame, int target, int current, String desc) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            // Re-adjuntamos las entidades para evitar problemas de sesiones diferentes en Hibernate
            User mergedCreator = session.merge(creator);
            Videogame mergedGame = session.merge(videogame);

            Offer offer = new Offer();
            offer.setCreator(mergedCreator);
            offer.setVideogame(mergedGame);
            offer.setTargetPlayersCount(target);
            offer.setCurrentPlayersCount(current);
            offer.setDescription(desc);
            offer.setFull(false);

            session.persist(offer);
            session.getTransaction().commit();
            return offer;
        }
    }

    @Test
    void searchOffersWithFiltersTest() {
        // --- 1. RECUPERAR VIDEOJUEGOS YA EXISTENTES ---
        // (IMPORTANTE: Cambia "Valorant" y "Minecraft" por dos títulos reales que tengas insertados)
        Videogame game1 = getExistingVideogame("Valorant");
        Videogame game2 = getExistingVideogame("Minecraft");

        assertNotNull(game1, "El videojuego 'Valorant' debe existir previamente en la BD");
        assertNotNull(game2, "El videojuego 'Minecraft' debe existir previamente en la BD");

        // --- 2. CREACIÓN DE USUARIOS (Mismo creador o distintos para las ofertas) ---
        User creator = createTestUser("offer_creator", "creator@test.com", "1234");
        User user2 = createTestUser("other_user", "other@test.com", "1234");

        // Oferta 1: Juego 1, Máximo 5 jugadores, 1 actual (Creada por 'creator')
        createTestOffer(creator, game1, 5, 1, "Busco equipo serio");

        // Oferta 2: Juego 2, Máximo 10 jugadores, 3 actuales (Creada por 'creator' también)
        createTestOffer(creator, game2, 10, 3, "Partida chill en server");

        // Oferta 3: Juego 1, Máximo 2 jugadores, 1 actual (Creada por 'user2')
        createTestOffer(user2, game1, 2, 1, "Duo competitivo para subir");


        // --- PRUEBA 1: Buscar sin filtros (Debe traer las 3 ofertas activas) ---
        String noFiltersJson = "{\"action\":\"SEARCH_OFFERS\",\"data\":{}}";
        String response1Json = messageProcessor.processMessage(noFiltersJson, null);
        DataPackage response1 = messageProcessor.jsonToPackage(response1Json);

        assertEquals("SEARCH_OFFERS_SUCCESS", response1.getAction());
        List<Map<String, Object>> offers1 = (List<Map<String, Object>>) response1.getData().get("offers");
        assertEquals(3, offers1.size(), "Debería devolver todas las ofertas activas");


        // --- PRUEBA 2: Filtrar por Título de Videojuego ("Valorant") ---
        String titleFilterJson = "{\"action\":\"SEARCH_OFFERS\",\"data\":{\"videogame_title\":\"Valorant\"}}";
        String response2Json = messageProcessor.processMessage(titleFilterJson, null);
        DataPackage response2 = messageProcessor.jsonToPackage(response2Json);

        List<Map<String, Object>> offers2 = (List<Map<String, Object>>) response2.getData().get("offers");
        assertEquals(2, offers2.size(), "Deberían ser 2 ofertas de Valorant");
        assertEquals("Valorant", offers2.get(0).get("videogame_title"));
        assertEquals("Valorant", offers2.get(1).get("videogame_title"));


        // --- PRUEBA 3: Filtrar por Máximo de Jugadores (max_target_players <= 5) ---
        // Como en tu MessageProcessor parseas los datos como (String), los enviamos en el JSON como texto
        String maxPlayersFilterJson = "{\"action\":\"SEARCH_OFFERS\",\"data\":{\"max_target_players\":\"5\"}}";
        String response3Json = messageProcessor.processMessage(maxPlayersFilterJson, null);
        DataPackage response3 = messageProcessor.jsonToPackage(response3Json);

        List<Map<String, Object>> offers3 = (List<Map<String, Object>>) response3.getData().get("offers");
        assertEquals(2, offers3.size(), "Debería excluir la oferta de Minecraft que pide 10");


        // --- PRUEBA 4: Filtros Combinados (Minecraft + Mínimo 3 jugadores actuales) ---
        String combinedFilterJson = "{\"action\":\"SEARCH_OFFERS\",\"data\":{\"videogame_title\":\"Minecraft\", \"min_current_players\":\"3\"}}";
        String response4Json = messageProcessor.processMessage(combinedFilterJson, null);
        DataPackage response4 = messageProcessor.jsonToPackage(response4Json);

        List<Map<String, Object>> offers4 = (List<Map<String, Object>>) response4.getData().get("offers");
        assertEquals(1, offers4.size());
        assertEquals("Partida chill en server", offers4.get(0).get("description"));
    }
}