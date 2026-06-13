package flores.caro;

import flores.caro.model.DataPackage;
import flores.caro.model.entities.Chat;
import flores.caro.model.entities.Message;
import flores.caro.model.entities.Offer;
import flores.caro.model.entities.User;
import flores.caro.model.entities.Videogame;
import flores.caro.utils.DBDAO;
import flores.caro.utils.OfferCreationResult;
import flores.caro.utils.OfferJoiningResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Instant;
import java.util.*;

public class MessageProcessor {
    private DBDAO dao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageProcessor(DBDAO dao) {
        this.dao = dao;
    }

    public String processMessage(String json, ClientHandler clientHandler) {
        DataPackage requestPackage = jsonToPackage(json);
        DataPackage responsePackage = new DataPackage();

        switch (requestPackage.getAction()) {
            case "LOGIN":
                responsePackage = logIn(requestPackage, clientHandler);
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
                responsePackage = searchOffers(requestPackage);
                break;
            case "GET_VIDEOGAMES":
                responsePackage = getVideogames();
                break;
            case "LEAVE_CHAT":
                responsePackage = leaveChat(requestPackage);
                break;
            case "SEND_MESSAGE":
                responsePackage = sendMessage(requestPackage);
                break;
            case "GET_CHAT_HISTORY":
                responsePackage = getChatHistory(requestPackage);
                break;
            case "GET_USER_CHATS":
                responsePackage = getUserChats(requestPackage);
                break;
            case "GET_CURRENT_OFFER":
                responsePackage = getCurrentOffer(requestPackage);
                break;
            case "GET_USER_DATA":
                responsePackage = getUserData(requestPackage);
                break;
            case "UPDATE_USER":
                responsePackage = updateUser(requestPackage);
                break;

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




    // =====================================================
    // *** JSON ***
    // =====================================================

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




    // =====================================================
    // *** LOGIN / SIGNUP ***
    // =====================================================

    private DataPackage logIn(DataPackage requestPackage, ClientHandler clientHandler) {
        DataPackage response = new DataPackage();
        try {
            String username = (String) requestPackage.getData().get("username");
            String password = (String) requestPackage.getData().get("password");

            if (dao.existsUsername(username)) {
                if (dao.checkCredentials(username, password)) {
                    Integer userId = dao.getUserIdByUsername(username);

                    if (SessionManager.getSession(userId) == null) {
                        String role = dao.getUserRole(userId);

                        response.setAction("LOGIN_SUCCESS");
                        response.setData(Map.of(
                                "message", "Login successful",
                                "user_id", userId,
                                "username", username,
                                "role", role
                        ));

                        SessionManager.addSession(userId, clientHandler);
                        clientHandler.setUserId(userId);
                    } else {
                        response.setAction("LOGIN_ERROR");
                        response.setData(Map.of("message", "User already has an active session"));
                    }
                } else {
                    response.setAction("LOGIN_ERROR");
                    response.setData(Map.of("message", "Wrong password for this username"));
                }
            } else {
                response.setAction("LOGIN_ERROR");
                response.setData(Map.of("message", "Username doesn't exist"));
            }

        } catch (ClassCastException e) {
            System.err.println("Class cast exception on login" + e.getMessage());
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
                response.setAction("SIGNUP_ERROR");
                response.setData(Map.of("message", "Error trying to create account"));
            }

        } catch (ClassCastException e) {
            System.err.println("Class cast exception on signup" + e.getMessage());
            response.setAction("SIGNUP_ERROR");
            response.setData(Map.of("message", "Invalid data format received"));
        } catch (Exception e) {
            System.err.println("SERVER ERROR ON SIGN UP: " + e.getMessage());
            response.setAction("SIGNUP_ERROR");
            response.setData(Map.of("message", "Internal server error during registration"));
        }

        return response;
    }




    // =====================================================
    // *** VIDEOGAMES ***
    // =====================================================

    private DataPackage getVideogames() {
        DataPackage response = new DataPackage();
        try {
            List<Videogame> videogames = dao.getVideogames();
            if (videogames == null) {
                response.setAction("GET_VIDEOGAMES_ERROR");
                response.setData(Map.of("message", "Error retrieving videogames"));
            } else {
                List<Map<String, Object>> videogameList = new ArrayList<>();
                for (Videogame v : videogames) {
                    videogameList.add(Map.of(
                            "id", v.getId(),
                            "title", v.getTitle(),
                            "category", v.getCategory()
                    ));
                }
                response.setAction("GET_VIDEOGAMES_SUCCESS");
                response.setData(Map.of("videogames", videogameList));
            }
        } catch (Exception e) {
            response.setAction("GET_VIDEOGAMES_ERROR");
            response.setData(Map.of("message", "Internal server error retrieving videogames"));
        }
        return response;
    }


    // =====================================================
    // *** OFFERS ***
    // =====================================================

    // Recordar en cliente mandar "creator": {"id": "1"}
    // Mandar creator como objeto User con atributo id solo, pero no "creator_id": "1"
    private DataPackage createOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Offer offer = objectMapper.convertValue(requestPackage.getData(), Offer.class);

            OfferCreationResult result = dao.createOffer(offer);

            switch (result) {
                case SUCCESS:
                    response.setAction("CREATE_OFFER_SUCCESS");
                    response.setData(Map.of(
                            "message", "Offer and Chat created successfully",
                            "offer_id", offer.getId(),
                            "chat_id", offer.getChat().getId(),
                            "chat_name", offer.getChat().getName()
                    ));
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
                    Chat chat = dao.getOfferChat(offerId);
                    response.setAction("JOIN_OFFER_SUCCESS");
                    response.setData(Map.of(
                            "message", "Joined offer successfully",
                            "chat_id", chat.getId(),
                            "chat_name", chat.getName() != null ? chat.getName() : "Chat"
                    ));
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
                case OFFER_FULL:
                    response.setAction("JOIN_OFFER_ERROR");
                    response.setData(Map.of("message", "Could not join offer, the offer is already full"));
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

    // Sólo las ofertas que no estén llenas
    // Y que no sean las del propio usuario
    private DataPackage searchOffers(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            List<Offer> offers;
            Integer userId = (Integer) requestPackage.getData().get("user_id");
            // Filters tiene como valor un json anidado con cada filtro específico
            Map<String, Object> filters = (Map<String, Object>) requestPackage.getData().get("filters");
            // Si es null es que no hay ningún filtro, buscamos todas las ofertas

            Integer videogameId = (Integer) filters.get("videogame_id");
            String videogameCategory = (String) filters.get("videogame_category");
            String offerMaxTargetPlayers = (String) filters.get("max_target_players");
            String offerMinCurrentPlayers = (String) filters.get("min_current_players");
            offers = dao.getFilteredOffers(userId, videogameId, videogameCategory, offerMaxTargetPlayers, offerMinCurrentPlayers);

            if (offers != null) {
                List<Map<String, Object>> offersData = new ArrayList<>();

                for (Offer offer : offers) {
                    offersData.add(Map.of(
                            "offer_id", offer.getId(),
                            "description", offer.getDescription() != null ? offer.getDescription() : "",
                            "current_players", offer.getCurrentPlayersCount(),
                            "target_players", offer.getTargetPlayersCount(),
                            "videogame_id", offer.getVideogame().getId(),
                            "videogame_title", offer.getVideogame().getTitle(),
                            "creator_id", offer.getCreator().getId(),
                            "creator_username", offer.getCreator().getUsername()
                    ));
                }
                response.setAction("SEARCH_OFFERS_SUCCESS");
                response.setData(Map.of("offers", offersData));
            } else {
                response.setAction("SEARCH_OFFERS_ERROR");
                response.setData(Map.of("message", "Couldn't get offers from database"));
            }

        } catch (ClassCastException e) {
            System.err.println("Casting error in search parameters: " + e.getMessage());
            response.setAction("SEARCH_OFFERS_ERROR");
            response.setData(Map.of("message", "Invalid parameter types format"));
        } catch (NullPointerException e) {
            System.err.println("Null Pointer Exception: " + e.getMessage());
            response.setAction("SEARCH_OFFERS_ERROR");
            response.setData(Map.of("message", "Missing or null required data"));
        } catch (Exception e) {
            // Final safety net for completely unexpected runtime issues
            System.err.println("Unexpected error during search: " + e.getMessage());
            e.printStackTrace();
            response.setAction("SEARCH_OFFERS_ERROR");
            response.setData(Map.of("message", "Internal server error searching offers"));
        }

        return response;
    }



    // =====================================================
    // *** CHATS ***
    // =====================================================

    private DataPackage leaveChat(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer chatId = (Integer) requestPackage.getData().get("chat_id");
            Integer userId = (Integer) requestPackage.getData().get("user_id");

            if (dao.leaveChat(chatId, userId)) {
                response.setAction("LEAVE_CHAT_SUCCESS");
                response.setData(Map.of("message", "Left the chat successfully"));
            } else {
                response.setAction("LEAVE_CHAT_ERROR");
                response.setData(Map.of("message", "Error trying to leave the chat"));
            }

        } catch (Exception e) {
            response.setAction("LEAVE_CHAT_ERROR");
            response.setData(Map.of("message", "Invalid arguments types received for leave chat"));
        }

        return response;
    }

    private DataPackage sendMessage(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Message message = objectMapper.convertValue(requestPackage.getData(), Message.class);
            message.setTimestamp(Instant.now());

            Message realMessage = dao.registerMessage(message); // Habiendo asignado el usuario y el chat reales
            broadcastChat(realMessage);

            response.setAction("SEND_MESSAGE_SUCCESS");
            response.setData(Map.of("message", "Message broadcasted to chat successfully"));

        } catch (IllegalArgumentException | MismatchedInputException e) {
            response.setAction("SEND_MESSAGE_ERROR");
            response.setData(Map.of("message", "Invalid data format received for message"));
        }

        return response;
    }

    private void broadcastChat(Message message) {
        Set<User> users = dao.getChatUsersFromMessage(message);

        DataPackage broadcastPackage = new DataPackage();
        broadcastPackage.setAction("NEW_CHAT_MESSAGE");
        // Hecho así para evitar bucles de JSON (Message -> Chat -> Messages/Offer -> Chat // User -> Offer...
        // y para no enviar datos innecesarios
        broadcastPackage.setData(Map.of(
                "message_id", message.getId(),
                "user_id", message.getUser().getId(),
                "chat_id", message.getChat().getId(),
                "timestamp", message.getTimestamp(),
                "text", message.getText(),
                "user_username", message.getUser().getUsername()
        ));

        // Broadcast A TÓDO EL MUNDO, incluyendo el emisor
        for (User user : users) {
            ClientHandler clientHandler = SessionManager.getSession(user.getId());
            if (clientHandler != null)
                clientHandler.sendMessage(packageToJson(broadcastPackage));
        }

    }

    private DataPackage getUserChats(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer userId = (Integer) requestPackage.getData().get("user_id");
            List<Chat> chats = dao.getUserChats(userId);

            if (chats != null) {
                List<Map<String, Object>> chatsData = new ArrayList<>();

                for (Chat chat : chats) {
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("chat_id", chat.getId());
                    chatData.put("name", chat.getName() != null ? chat.getName() : "Chat");
                    chatsData.add(chatData);
                }

                response.setAction("GET_USER_CHATS_SUCCESS");
                response.setData(Map.of("chats", chatsData));
            } else {
                response.setAction("GET_USER_CHATS_ERROR");
                response.setData(Map.of("message", "Could not get user chats from database"));
            }

        } catch (ClassCastException e) {
            response.setAction("GET_USER_CHATS_ERROR");
            response.setData(Map.of("message", "Invalid format for get user chats"));
        } catch (Exception e) {
            response.setAction("GET_USER_CHATS_ERROR");
            response.setData(Map.of("message", "Internal error trying to get user chats"));
        }

        return response;
    }

    private DataPackage getCurrentOffer(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer userId = (Integer) requestPackage.getData().get("user_id");
            Offer offer = dao.getCurrentOffer(userId);

            if (offer != null) {
                response.setAction("GET_CURRENT_OFFER_SUCCESS");
                response.setData(Map.of(
                        "offer_id", offer.getId(),
                        "description", offer.getDescription() != null ? offer.getDescription() : "",
                        "current_players", offer.getCurrentPlayersCount(),
                        "target_players", offer.getTargetPlayersCount(),
                        "videogame_id", offer.getVideogame().getId(),
                        "videogame_title", offer.getVideogame().getTitle(),
                        "creator_id", offer.getCreator().getId(),
                        "creator_username", offer.getCreator().getUsername()
                ));
            } else {
                response.setAction("GET_CURRENT_OFFER_NOT_FOUND");
                response.setData(Map.of("message", "User has no active offer"));
            }

        } catch (ClassCastException e) {
            response.setAction("GET_CURRENT_OFFER_ERROR");
            response.setData(Map.of("message", "Invalid format for get current offer"));
        } catch (Exception e) {
            response.setAction("GET_CURRENT_OFFER_ERROR");
            response.setData(Map.of("message", "Internal error trying to get current offer"));
        }

        return response;
    }

    // =====================================================
    // *** USER ***
    // =====================================================

    private DataPackage getUserData(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer userId = (Integer) requestPackage.getData().get("user_id");
            User user = dao.getUserById(userId);

            if (user != null) {
                response.setAction("GET_USER_DATA_SUCCESS");
                response.setData(Map.of(
                        "user_id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                ));
            } else {
                response.setAction("GET_USER_DATA_ERROR");
                response.setData(Map.of("message", "User not found"));
            }

        } catch (ClassCastException e) {
            response.setAction("GET_USER_DATA_ERROR");
            response.setData(Map.of("message", "Invalid format for get user data"));
        } catch (Exception e) {
            response.setAction("GET_USER_DATA_ERROR");
            response.setData(Map.of("message", "Internal error trying to get user data"));
        }

        return response;
    }

    private DataPackage updateUser(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer userId = (Integer) requestPackage.getData().get("user_id");
            String newUsername = (String) requestPackage.getData().get("username");
            String newEmail = (String) requestPackage.getData().get("email");
            String newPassword = (String) requestPackage.getData().get("password");

            User currentUser = dao.getUserById(userId);
            if (currentUser == null) {
                response.setAction("UPDATE_USER_ERROR");
                response.setData(Map.of("message", "User not found"));
                return response;
            }

            // Que no lo tenga ya otro usuario
            if (newUsername != null && !newUsername.equals(currentUser.getUsername())) {
                if (dao.existsUsername(newUsername)) {
                    response.setAction("UPDATE_USER_ERROR");
                    response.setData(Map.of("message", "Username is already taken"));
                    return response;
                }
            }

            if (newEmail != null && !newEmail.equals(currentUser.getEmail())) {
                if (dao.existsEmail(newEmail)) {
                    response.setAction("UPDATE_USER_ERROR");
                    response.setData(Map.of("message", "Email is already registered"));
                    return response;
                }
            }

            if (dao.updateUser(userId, newUsername, newEmail, newPassword)) {
                response.setAction("UPDATE_USER_SUCCESS");
                response.setData(Map.of("message", "User updated successfully"));
            } else {
                response.setAction("UPDATE_USER_ERROR");
                response.setData(Map.of("message", "Internal error updating user"));
            }

        } catch (ClassCastException e) {
            response.setAction("UPDATE_USER_ERROR");
            response.setData(Map.of("message", "Invalid format for update user"));
        } catch (Exception e) {
            response.setAction("UPDATE_USER_ERROR");
            response.setData(Map.of("message", "Internal error trying to update user"));
        }

        return response;
    }

    private DataPackage getChatHistory(DataPackage requestPackage) {
        DataPackage response = new DataPackage();

        try {
            Integer chatId = (Integer) requestPackage.getData().get("chat_id");
            List<Message> messages = dao.getChatHistory(chatId);

            if (messages != null) {
                // Lista de mensajes. Cada Map es un mensaje
                List<Map<String, Object>> messagesData = new ArrayList<>();

                for (Message message : messages) {
                    // Creamos mapa de cada mensaje
                    messagesData.add(Map.of(
                            "message_id", message.getId(),
                            "user_id", message.getUser().getId(),
                            "chat_id", message.getChat().getId(),
                            "timestamp", message.getTimestamp(),
                            "text", message.getText(),
                            "user_username", message.getUser().getUsername()
                    ));
                }

                response.setAction("GET_CHAT_HISTORY_SUCCESS");
                response.setData(Map.of("messages", messagesData));
            } else {
                response.setAction("GET_CHAT_HISTORY_ERROR");
                response.setData(Map.of("message", "Could not get chat history from database"));
            }

        } catch (ClassCastException e) {
            response.setAction("GET_CHAT_HISTORY_ERROR");
            response.setData(Map.of("message", "Invalid format requested for chat history"));
        } catch (Exception e) {
            response.setAction("GET_CHAT_HISTORY_ERROR");
            response.setData(Map.of("message", "Internal error trying to get chat history"));
        }

        return response;
    }
}