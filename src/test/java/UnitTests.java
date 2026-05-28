import flores.caro.MessageProcessor;
import flores.caro.model.DataPackage;
import flores.caro.model.entities.User;
import flores.caro.utils.DBDAO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitTests {
    @Test
    void daoLoginTest() {
        DBDAO dao = new DBDAO();
        boolean expected = true;
        boolean actual = dao.checkCredentials("pacocucaracha", "lorenzo");

        assertEquals(expected, actual);
    }

    @Test
    void messageProcessorLoginTest() {
        DBDAO dao = new DBDAO();
        MessageProcessor mp = new MessageProcessor(dao);

        boolean expected = true;
        // boolean actual = mp.processMessage();   // Hacer deserializar y demás
    }

    @Test
    void jsonToPackageTest() {
        String json = "{ " +
                        "\"action\": \"LOGIN\", " +
                        "\"data\": {" +
                        "   \"username\": \"pacoercucaracha\", " +
                        "   \"password\": \"holapaco\"" +
                        "}" +
                    "}";

        DBDAO dao = new DBDAO();
        MessageProcessor mp = new MessageProcessor(dao);
        mp.jsonToPackage(json);
    }

    @Test
    void daoRegisterUserTest() {
        DBDAO dao = new DBDAO();
        dao.registerUser("hola@gmail.com", "user1", "1234");
    }

    @Test
    void signUpTest() {
        String json = "{ " +
                "\"action\": \"SIGNUP\", " +
                "\"data\": {" +
                "   \"email\": \"hola2@gmail.com\", " +
                "   \"username\": \"user2\", " +
                "   \"password\": \"1234\"" +
                "}" +
            "}";

        DBDAO dao = new DBDAO();
        MessageProcessor mp = new MessageProcessor(dao);

        String response = mp.processMessage(json);
        DataPackage responseDataPackage = mp.jsonToPackage(response);

        System.out.println(responseDataPackage.getAction());
        System.out.println(responseDataPackage.getData());
    }



}
