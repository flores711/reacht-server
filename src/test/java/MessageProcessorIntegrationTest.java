import flores.caro.MessageProcessor;
import flores.caro.model.DataPackage;
import flores.caro.model.entities.User;
import flores.caro.utils.DBDAO;
import flores.caro.utils.HibernateUtil;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Unknown action requested", (String) response.getData().get("message"));
    }

    @Test
    void malformedJsonTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\"}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Malformed JSON or deserialization error", (String) response.getData().get("message"));
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void loginSuccessTest() {
        createTestUser("testuser", "testuser@example.com", "password123");

        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"password123\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_SUCCESS", response.getAction());
        assertEquals("testuser", (String) response.getData().get("username"));
        assertEquals("NORMAL", (String) response.getData().get("role"));
    }

    @Test
    void loginWrongPasswordTest() {
        createTestUser("testuser", "testuser@example.com", "password123");

        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"wrong_password\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Wrong password for this username", (String) response.getData().get("message"));
    }

    @Test
    void loginUserDoesNotExistTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"fakeuser999\",\"password\":\"password123\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Username doesn't exist", (String) response.getData().get("message"));
    }

    // ==========================================
    // SIGN UP TESTS
    // ==========================================

    @Test
    void signUpSuccessTest() {
        String requestJson = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"newuser@example.com\",\"username\":\"newuser\",\"password\":\"newpassword123\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_SUCCESS", response.getAction());
        assertEquals("Account created successfully", (String) response.getData().get("message"));
    }

    @Test
    void signUpEmailAlreadyExistsTest() {
        // Registramos al primer usuario
        String requestJson1 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"testuser@example.com\",\"username\":\"user_one\",\"password\":\"password123\"}}";
        messageProcessor.processMessage(requestJson1);

        // Intentamos registrar a otro usuario con distinto username pero el mismo EMAIL
        String requestJson2 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"testuser@example.com\",\"username\":\"user_two\",\"password\":\"password123\"}}";
        String responseJson = messageProcessor.processMessage(requestJson2);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_ERROR", response.getAction());
        assertEquals("Email is already registered", (String) response.getData().get("message"));
    }

    @Test
    void signUpUsernameAlreadyExistsTest() {
        // Registramos al primer usuario
        String requestJson1 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"first@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}}";
        messageProcessor.processMessage(requestJson1);

        // Intentamos registrar a otro usuario con distinto email pero el mismo USERNAME
        String requestJson2 = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"second@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}}";
        String responseJson = messageProcessor.processMessage(requestJson2);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("SIGNUP_ERROR", response.getAction());
        assertEquals("Username is already taken", (String) response.getData().get("message"));
    }

    // ==========================================
    // CREATE OFFER TESTS
    // ==========================================

    @Test
    void createOfferSuccessTest() {
        // Creamos el usuario que va a publicar la oferta
        User creator = createTestUser("offercreator", "creator@test.com", "1234");

        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Chill game\",\"creator\":{\"id\":" + creator.getId() + "}}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_SUCCESS", response.getAction());
        assertEquals("Offer and Chat created successfully", (String) response.getData().get("message"));
    }

    @Test
    void createOfferUserAlreadyHasActiveOfferTest() {
        User creator = createTestUser("offercreator", "creator@test.com", "1234");
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Chill game\",\"creator\":{\"id\":" + creator.getId() + "}}}";

        // Primer intento: crea la oferta con éxito
        messageProcessor.processMessage(requestJson);

        // Segundo intento: debe fallar porque ya tiene una activa
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("Could not create offer. User already has an active one", (String) response.getData().get("message"));
    }

    @Test
    void createOfferUserNotFoundTest() {
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Ghost game\",\"creator\":{\"id\":999999}}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("The user does not exist in the database", (String) response.getData().get("message"));
    }

    @Test
    void createOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Bad game\",\"creator\":\"This is not an ID object\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("CREATE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for offer", (String) response.getData().get("message"));
    }

    // ==========================================
    // DELETE OFFER TESTS
    // ==========================================

    @Test
    void deleteOfferSuccessTest() {
        User creator = createTestUser("deleter", "deleter@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Delete test offer\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson);

        // Coger id de la oferta creada de la BD (porque es automática)
        int offerId = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":" + offerId + "}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_SUCCESS", response.getAction());
        assertEquals("Offer deleted successfully", (String) response.getData().get("message"));
    }

    @Test
    void deleteOfferNotFoundTest() {
        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":999999}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_ERROR", response.getAction());
        assertEquals("Error deleting the offer", (String) response.getData().get("message"));
    }

    @Test
    void deleteOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"DELETE_OFFER\",\"data\":{\"offer_id\":\"not a number\"}}";

        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("DELETE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for delete offer", (String) response.getData().get("message"));
    }

    // ==========================================
    // JOIN OFFER TESTS
    // ==========================================

    @Test
    void joinOfferSuccessTest() {
        // Creamos el creador y su oferta
        User creator = createTestUser("creator_join", "creator_join@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Join me\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson);

        // Cogemos el ID de la oferta creada
        int offerId = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Usuario que se va a unir
        User joiner = createTestUser("joiner", "joiner@test.com", "1234");

        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + joiner.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_SUCCESS", response.getAction());
        assertEquals("Joined offer successfully", (String) response.getData().get("message"));
    }

    @Test
    void joinOfferOfferNotFoundTest() {
        User joiner = createTestUser("joiner", "joiner@test.com", "1234");

        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":999999,\"user_id\":" + joiner.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("The offer no longer exists", (String) response.getData().get("message"));
    }

    @Test
    void joinOfferUserNotFoundTest() {
        // Creamos el creador y su oferta
        User creator = createTestUser("creator_join", "creator_join@test.com", "1234");
        String createOfferJson = "{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Join me\",\"creator\":{\"id\":" + creator.getId() + "}}}";
        messageProcessor.processMessage(createOfferJson);

        int offerId = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Intentamos unirnos con usuario que no existe
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":999999}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("The user does not exist in the database", (String) response.getData().get("message"));
    }

    @Test
    void joinOfferUserAlreadyInAnOfferTest() {
        // Oferta A
        User creatorA = createTestUser("creatorA", "creatorA@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Offer A\",\"creator\":{\"id\":" + creatorA.getId() + "}}}");

        int offerIdA = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creatorA.getId());
            offerIdA = u.getCurrentOffer().getId();
        }

        // Usuario B y oferta B (y usuario B ya está en una oferta activa)
        User creatorB = createTestUser("creatorB", "creatorB@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Offer B\",\"creator\":{\"id\":" + creatorB.getId() + "}}}");

        // Usuario B intenta unirse a la oferta A
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerIdA + ",\"user_id\":" + creatorB.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("Could not join offer. User is already in an active offer", (String) response.getData().get("message"));
    }

    @Test
    void joinOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":\"invalid\",\"user_id\":\"invalid\"}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("JOIN_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for join offer", (String) response.getData().get("message"));
    }

    // ==========================================
    // LEAVE OFFER TESTS
    // ==========================================

    @Test
    void leaveOfferSuccessTest() {
        // Creador hace la oferta
        User creator = createTestUser("creator_leave", "creator_leave@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Leave me\",\"creator\":{\"id\":" + creator.getId() + "}}}");

        int offerId = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Jugador se une a la oferta
        User leaver = createTestUser("leaver", "leaver@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"JOIN_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + leaver.getId() + "}}");

        // Jugador abandona la oferta
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + leaver.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_SUCCESS", response.getAction());
        assertEquals("Left the offer successfully", (String) response.getData().get("message"));
    }

    @Test
    void leaveOfferErrorNotAMemberTest() {
        // Creador hace la oferta
        User creator = createTestUser("creator_leave2", "creator_leave2@test.com", "1234");
        messageProcessor.processMessage("{\"action\":\"CREATE_OFFER\",\"data\":{\"description\":\"Leave me\",\"creator\":{\"id\":" + creator.getId() + "}}}");

        int offerId = -1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User u = session.get(User.class, creator.getId());
            offerId = u.getCurrentOffer().getId();
        }

        // Creamos un usuario pero NO lo unimos a la oferta
        User stranger = createTestUser("stranger", "stranger@test.com", "1234");

        // El usuario intenta abandonar una oferta en la que no está
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":" + offerId + ",\"user_id\":" + stranger.getId() + "}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_ERROR", response.getAction());
        assertEquals("Error trying to leave the offer", (String) response.getData().get("message"));
    }

    @Test
    void leaveOfferInvalidDataFormatTest() {
        String requestJson = "{\"action\":\"LEAVE_OFFER\",\"data\":{\"offer_id\":\"invalid\",\"user_id\":\"invalid\"}}";
        String responseJson = messageProcessor.processMessage(requestJson);
        DataPackage response = messageProcessor.jsonToPackage(responseJson);

        assertEquals("LEAVE_OFFER_ERROR", response.getAction());
        assertEquals("Invalid arguments types received for leave offer", (String) response.getData().get("message"));
    }
}