import flores.caro.MessageProcessor;
import flores.caro.model.DataPackage;
import flores.caro.utils.DBDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageProcessorIntegrationTest {

    private MessageProcessor mp;

    @BeforeEach
    void setUp() {
        DBDAO dao = new DBDAO();
        mp = new MessageProcessor(dao);
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void loginSuccessTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"password123\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("LOGIN_SUCCESS", response.getAction());
        assertEquals("testuser", response.getData().get("username"));
        assertEquals("NORMAL", response.getData().get("role"));
    }

    @Test
    void loginWrongPasswordTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"wrong_password\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Wrong password for this username", response.getData().get("message"));
    }

    @Test
    void loginUserDoesNotExistTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"fakeuser999\",\"password\":\"password123\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("LOGIN_ERROR", response.getAction());
        assertEquals("Username doesn't exist", response.getData().get("message"));
    }

    // ==========================================
    // SIGN UP TESTS
    // ==========================================

    @Test
    void signUpSuccessTest() {
        String requestJson = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"newuser@example.com\",\"username\":\"newuser\",\"password\":\"newpassword123\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("SIGN_UP_SUCCESS", response.getAction());
        assertEquals("Account created successfully.", response.getData().get("message"));
    }

    @Test
    void signUpEmailAlreadyExistsTest() {
        String requestJson = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"testuser@example.com\",\"username\":\"anotheruser\",\"password\":\"password123\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("SIGN_UP_ERROR", response.getAction());
        assertEquals("Email is already registered.", response.getData().get("message"));
    }

    @Test
    void signUpUsernameAlreadyExistsTest() {
        String requestJson = "{\"action\":\"SIGNUP\",\"data\":{\"email\":\"another@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("SIGN_UP_ERROR", response.getAction());
        assertEquals("Username is already taken.", response.getData().get("message"));
    }

    // ==========================================
    // STRUCTURE AND ERROR TESTS
    // ==========================================

    @Test
    void unknownActionTest() {
        String requestJson = "{\"action\":\"DELETE_ACCOUNT\",\"data\":{\"username\":\"testuser\"}}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Unknown action requested.", response.getData().get("message"));
    }

    @Test
    void malformedJsonTest() {
        String requestJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\"}";

        String responseJson = mp.processMessage(requestJson);
        DataPackage response = mp.jsonToPackage(responseJson);

        assertEquals("ERROR", response.getAction());
        assertEquals("Malformed JSON or deserialization error.", response.getData().get("message"));
    }
}