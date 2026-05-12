import flores.caro.MessageProcessor;
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

}
