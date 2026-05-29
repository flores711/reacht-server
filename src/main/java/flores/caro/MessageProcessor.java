package flores.caro;

import flores.caro.model.DataPackage;
import flores.caro.utils.DBDAO;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

public class MessageProcessor {
    private DBDAO dao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageProcessor(DBDAO dao) {
        this.dao = dao;
    }

    public String processMessage(String json) {
        DataPackage requestPackage = jsonToPackage(json);
        DataPackage responsePackage;

        switch (requestPackage.getAction()) {
            case "LOGIN":
                responsePackage = logIn(requestPackage);
                break;
            case "SIGNUP":
                responsePackage = signUp(requestPackage);
                break;
            case "ERROR":
                // Devolvemos lo que nos ha devuelto el jsonToPackage si ha ido mal, que almacenamos en requestPackage
                responsePackage = requestPackage;
                break;
            default:
                responsePackage = new DataPackage();
                responsePackage.setAction("ERROR");
                responsePackage.setData(Map.of("message", "Unknown action requested."));
                break;
        }

        return packageToJson(responsePackage);
    }

    public DataPackage jsonToPackage(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        DataPackage response = new DataPackage();

        try {
            response = objectMapper.readValue(json, DataPackage.class);
        } catch (Exception e) {
            response.setAction("ERROR");
            response.setData(Map.of("message", "Malformed JSON or deserialization error."));
        }

        return response;
    }

    public String packageToJson(DataPackage dataPackage) {
        try {
            return objectMapper.writeValueAsString(dataPackage);
        } catch (Exception e) {
            System.err.println("Error serializing response: " + e.getMessage());
            return "{\"action\":\"ERROR\",\"data\":{\"message\":\"JSON serialization error.\"}}";
        }
    }


    private DataPackage logIn(DataPackage dataPackage) {
        DataPackage response = new DataPackage();
        try {
            String username = (String) dataPackage.getData().get("username");
            String password = (String) dataPackage.getData().get("password");

            // TODO: this has to be a separate thread
            if (dao.existsUsername(username)) {
                if (dao.checkCredentials(username, password)) {
                    response.setAction("LOGIN_SUCCESS");
                    // String role = dao.getUserRole();
                    String role = "NORMAL";

                    response.setData(Map.of(
                            "message", "Login successful",
                            "username", username,
                            "role", role
                    ));
                } else {
                    response.setAction("LOGIN_ERROR");
                    response.setData(Map.of("message", "Wrong password for this username"));
                }
            } else {
                response.setAction("LOGIN_ERROR");
                response.setData(Map.of("message", "Username doesn't exist"));
            }

        } catch (ClassCastException e) {
            System.err.println("CLASS CAST EXCEPTION ON LOG IN");
            response.setAction("LOGIN_ERROR");
            response.setData(Map.of("message", "Invalid data format received."));
        }

        return response;
    }

    public DataPackage signUp(DataPackage dataPackage) {
        DataPackage response = new DataPackage();

        try {
            String email = (String) dataPackage.getData().get("email");
            String username = (String) dataPackage.getData().get("username");
            String password = (String) dataPackage.getData().get("password");

            // Check email isolated
            if (dao.existsEmail(email)) {
                response.setAction("SIGN_UP_ERROR");
                response.setData(Map.of("message", "Email is already registered."));
                return response;
            }

            // Check username isolated
            if (dao.existsUsername(username)) {
                response.setAction("SIGN_UP_ERROR");
                response.setData(Map.of("message", "Username is already taken."));
                return response;
            }

            // Register if both checks pass
            dao.registerUser(email, username, password);

            response.setAction("SIGN_UP_SUCCESS");
            response.setData(Map.of("message", "Account created successfully."));

        } catch (ClassCastException e) {
            System.err.println("CLASS CAST EXCEPTION ON SIGN UP");
            response.setAction("SIGN_UP_ERROR");
            response.setData(Map.of("message", "Invalid data format received."));
        } catch (Exception e) {
            System.err.println("SERVER ERROR ON SIGN UP: " + e.getMessage());
            response.setAction("SIGN_UP_ERROR");
            response.setData(Map.of("message", "Internal server error during registration."));
        }

        return response;
    }
}
