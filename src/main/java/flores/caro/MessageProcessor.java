package flores.caro;

import flores.caro.model.DataPackage;
import flores.caro.model.entities.Chat;
import flores.caro.model.entities.Offer;
import flores.caro.utils.DBDAO;
import flores.caro.utils.OfferCreationResult;
import flores.caro.utils.OfferJoiningResult;
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
        DataPackage responsePackage = new DataPackage();

        switch (requestPackage.getAction()) {
            case "LOGIN":
                responsePackage = logIn(requestPackage);
                break;
            case "SIGNUP":
                responsePackage = signUp(requestPackage);
                break;
            case "CREATE_OFFER":
                responsePackage = createOffer(requestPackage);
                break;
            case "DELETE_OFFER":
                responsePackage = deleteOffer(requestPackage);
                break;
            case "JOIN_OFFER":
                responsePackage = joinOffer(requestPackage);
                break;
            case "LEAVE_OFFER":
                responsePackage = leaveOffer(requestPackage);
                break;
            case "SEARCH_OFFERS":
                // TODO
                break;
            case "FRIEND_REQUEST":
                // TODO
                break;
            case "FRIEND_REQUEST_ACCEPT":
                // TODO
                break;

            // Enviar mensajes chat

            case "ERROR":
                /* Devolvemos lo que nos ha devuelto el jsonToPackage si ha ido mal, que almacenamos en requestPackage
                No es que el cliente nos mande ERROR, sino que el jsonToPackage de su mensaje ha fallado
                y nos devuelve esto en vez del mensaje original */
                responsePackage = requestPackage;
                break;
            default:
                responsePackage = new DataPackage();
                responsePackage.setAction("ERROR");
                responsePackage.setData(Map.of("message", "Unknown action requested"));
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
            response.setData(Map.of("message", "Malformed JSON or deserialization error"));
        }

        return response;
    }

    public String packageToJson(DataPackage dataPackage) {
        try {
            return objectMapper.writeValueAsString(dataPackage);
        } catch (Exception e) {
            System.err.println("Error serializing response: " + e.getMessage());
            return "{\"action\":\"ERROR\",\"data\":{\"message\":\"JSON serialization error\"}}";
        }
    }


    private DataPackage logIn(DataPackage requestPackage) {
        DataPackage response = new DataPackage();
        try {
            String username = (String) requestPackage.getData().get("username");
            String password = (String) requestPackage.getData().get("password");

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
            response.setData(Map.of("message", "Invalid data format received"));
        }

        return response;
    }

    private DataPackage signUp(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            String email = (String) requestPackage.getData().get("email");
            String username = (String) requestPackage.getData().get("username");
            String password = (String) requestPackage.getData().get("password");

            // Check email isolated
            if (dao.existsEmail(email)) {
                response.setAction("SIGNUP_ERROR");
                response.setData(Map.of("message", "Email is already registered"));
                return response;
            }

            // Check username isolated
            if (dao.existsUsername(username)) {
                response.setAction("SIGNUP_ERROR");
                response.setData(Map.of("message", "Username is already taken"));
                return response;
            }

            // Register if both checks pass
            if (dao.registerUser(email, username, password)) {
                response.setAction("SIGNUP_SUCCESS");
                response.setData(Map.of("message", "Account created successfully"));
            } else {
                // TODO: Esto está bien?
                throw new Exception();
            }

        } catch (ClassCastException e) {
            System.err.println("CLASS CAST EXCEPTION ON SIGN UP");
            response.setAction("SIGNUP_ERROR");
            response.setData(Map.of("message", "Invalid data format received"));
        } catch (Exception e) {
            System.err.println("SERVER ERROR ON SIGN UP: " + e.getMessage());
            response.setAction("SIGNUP_ERROR");
            response.setData(Map.of("message", "Internal server error during registration"));
        }

        return response;
    }


    // Recordar en cliente mandar "creator": {"id": "1"}
    // Mandar creator como objeto User con atributo id solo, pero no "creator_id": "1"
    private DataPackage createOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Offer offer = objectMapper.convertValue(requestPackage.getData(), Offer.class);

            Chat offerChat = new Chat();
            offer.setChat(offerChat);

            OfferCreationResult result = dao.createOffer(offer);

            switch (result) {
                case SUCCESS:
                    response.setAction("CREATE_OFFER_SUCCESS");
                    response.setData(Map.of("message", "Offer and Chat created successfully"));
                    break;
                case USER_NOT_FOUND:
                    response.setAction("CREATE_OFFER_ERROR");
                    response.setData(Map.of("message", "The user does not exist in the database"));
                    break;
                case ALREADY_HAS_ACTIVE_OFFER:
                    response.setAction("CREATE_OFFER_ERROR");
                    response.setData(Map.of("message", "Could not create offer. User already has an active one"));
                    break;
                case DATABASE_ERROR:
                    response.setAction("CREATE_OFFER_ERROR");
                    response.setData(Map.of("message", "Internal server error saving the offer"));
                    break;
            }

        } catch (Exception e) {
            response.setAction("CREATE_OFFER_ERROR");
            response.setData(Map.of("message", "Invalid arguments types received for offer"));
        }

        return response;
    }

    private DataPackage deleteOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer offerId = (Integer) requestPackage.getData().get("offer_id");

            if (dao.deleteOffer(offerId)) {
                response.setAction("DELETE_OFFER_SUCCESS");
                response.setData(Map.of("message", "Offer deleted successfully"));
            } else {
                response.setAction("DELETE_OFFER_ERROR");
                response.setData(Map.of("message", "Error deleting the offer"));
            }

        } catch (Exception e) {
            response.setAction("DELETE_OFFER_ERROR");
            response.setData(Map.of("message", "Invalid arguments types received for delete offer"));
        }

        return response;
    }

    private DataPackage joinOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer offerId = (Integer) requestPackage.getData().get("offer_id");
            Integer userId = (Integer) requestPackage.getData().get("user_id");

            OfferJoiningResult result = dao.joinOffer(offerId, userId);

            switch (result) {
                case SUCCESS:
                    response.setAction("JOIN_OFFER_SUCCESS");
                    response.setData(Map.of("message", "Joined offer successfully"));
                    break;
                case OFFER_NOT_FOUND:
                    response.setAction("JOIN_OFFER_ERROR");
                    response.setData(Map.of("message", "The offer no longer exists"));
                    break;
                case USER_NOT_FOUND:
                    response.setAction("JOIN_OFFER_ERROR");
                    response.setData(Map.of("message", "The user does not exist in the database"));
                    break;
                case ALREADY_IN_AN_OFFER:
                    response.setAction("JOIN_OFFER_ERROR");
                    response.setData(Map.of("message", "Could not join offer. User is already in an active offer"));
                    break;
                case DATABASE_ERROR:
                    response.setAction("JOIN_OFFER_ERROR");
                    response.setData(Map.of("message", "Internal server error joining the offer"));
                    break;
            }

        } catch (Exception e) {
            response.setAction("JOIN_OFFER_ERROR");
            response.setData(Map.of("message", "Invalid arguments types received for join offer"));
        }

        return response;
    }

    private DataPackage leaveOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer offerId = (Integer) requestPackage.getData().get("offer_id");
            Integer userId = (Integer) requestPackage.getData().get("user_id");

            if (dao.leaveOffer(offerId, userId)) {
                response.setAction("LEAVE_OFFER_SUCCESS");
                response.setData(Map.of("message", "Left the offer successfully"));
            } else {
                response.setAction("LEAVE_OFFER_ERROR");
                response.setData(Map.of("message", "Error trying to leave the offer"));
            }

        } catch (Exception e) {
            response.setAction("LEAVE_OFFER_ERROR");
            response.setData(Map.of("message", "Invalid arguments types received for leave offer"));
        }

        return response;
    }
}
