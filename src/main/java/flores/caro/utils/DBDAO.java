package flores.caro.utils;

import flores.caro.model.entities.*;

import java.util.*;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DBDAO {
    private final SessionFactory sessionFactory;

    public DBDAO() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    // TODO: Capturar excepciones específicas, no solo Exception
    // En todo caso al final del todo, de más específicas a menos

    // ==============================================
    // *** QUERIES ***
    // ==============================================

    public boolean checkCredentials(String username, String password) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.username = :username AND u.password = :password", User.class);
            query.setParameter("username", username);
            query.setParameter("password", password);

            return !query.list().isEmpty();
        }
    }

    public User getUserById(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            return user;
        }
    }

    public Integer getUserIdByUsername(String username) {
        try (Session session = sessionFactory.openSession()) {
            Query<Integer> query = session.createQuery("SELECT u.id FROM User u WHERE u.username = :username", Integer.class);
            query.setParameter("username", username);
            Integer userId = query.uniqueResult();
            return userId;
        }
    }

    public boolean existsUsername(String username) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);

            return !query.list().isEmpty();
        }
    }

    public boolean existsEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);

            return !query.list().isEmpty();
        }
    }

    public boolean userHasActiveOffer(int creatorId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Offer> query = session.createQuery("SELECT u.currentOffer FROM User u WHERE u.id = :id", Offer.class);
            query.setParameter("id", creatorId);

            Offer offer = query.uniqueResult();
            return offer != null;
        }
    }

    public Set<User> getChatUsersFromMessage(Message message) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("SELECT users FROM Chat c JOIN c.users users WHERE c.id = :chat_id", User.class);
            query.setParameter("chat_id", message.getChat().getId());

            Set<User> users = new HashSet<>(query.list());
            return users;
        }
    }

    public List<Message> getChatHistory(Integer chatId) {
        try (Session session = sessionFactory.openSession()) {
            // Usamos JOIN FETCH para traer los datos del usuario emisor en la misma consulta
            Query<Message> query = session.createQuery("FROM Message m JOIN FETCH m.user WHERE m.chat.id = :chatId ORDER BY m.id ASC", Message.class);
            query.setParameter("chatId", chatId);

            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Chat getOfferChat(Integer offerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Chat> query = session.createQuery("SELECT o.chat FROM Offer o WHERE o.id = :offerId", Chat.class);
            query.setParameter("offerId", offerId);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Chat> getUserChats(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Chat> query = session.createQuery(
                    "FROM Chat c JOIN c.users u WHERE u.id = :userId",
                    Chat.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Offer getCurrentOffer(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null || user.getCurrentOffer() == null) return null;

            Query<Offer> query = session.createQuery(
                    "FROM Offer o JOIN FETCH o.videogame JOIN FETCH o.creator WHERE o.id = :offerId",
                    Offer.class);
            query.setParameter("offerId", user.getCurrentOffer().getId());
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Videogame> getVideogames() {
        try (Session session = sessionFactory.openSession()) {
            Query<Videogame> query = session.createQuery("FROM Videogame v ORDER BY v.title", Videogame.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("Internal server error obtaining videogames: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Offer> getAllOffers(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Offer> query = session.createQuery("FROM Offer o WHERE o.isFull = false AND o.creator.id != :userId", Offer.class);
            query.setParameter("userId", userId);
            query.setMaxResults(100);
            return query.list();
        } catch (HibernateException e) {
            System.err.println("Hibernate error obtaining all offers: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Internal server error obtaining all offers: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Offer> getFilteredOffers(Integer userId, Integer videogameId, String videogameCategory, String offerMaxTargetPlayers, String offerMinCurrentPlayers) {
        try (Session session = sessionFactory.openSession()) {

            StringBuilder querySB = new StringBuilder("FROM Offer o WHERE o.isFull = false AND o.creator.id != :userId");
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("userId", userId);

            if (videogameId != null) {
                querySB.append(" AND o.videogame.id = :videogameId");
                parameters.put("videogameId", videogameId);
            }
            if (videogameCategory != null) {
                querySB.append(" AND o.videogame.category = :videogameCategory");
                parameters.put("videogameCategory", videogameCategory);
            }
            if (offerMaxTargetPlayers != null) {
                querySB.append(" AND o.targetPlayersCount <= :offerMaxTargetPlayers");
                parameters.put("offerMaxTargetPlayers", offerMaxTargetPlayers);
            }
            if (offerMinCurrentPlayers != null) {
                querySB.append(" AND o.currentPlayersCount >= :offerMinCurrentPlayers");
                parameters.put("offerMinCurrentPlayers", offerMinCurrentPlayers);
            }

            Query<Offer> query = session.createQuery(querySB.toString(), Offer.class);
            // Se puede hacer esto porque son exactamente los mismos parámetros en el mismo orden
            parameters.forEach(query::setParameter);
            query.setMaxResults(100);

            List<Offer> filteredOffers = query.list();

            return filteredOffers;
        } catch (HibernateException e) {
            System.err.println("Hibernate error obtaining filtered offers: " + e.getMessage());
            e.printStackTrace();
            return null;

        } catch (Exception e) {
            System.err.println("Internal server error obtaining filtered offers: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }


    // ==============================================
    // *** MODIFICATIONS TO BD ***
    // ==============================================

    public boolean registerUser(String email, String username, String password) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(username);
                newUser.setPassword(password);

                Role playerRole = session.createQuery("FROM Role r WHERE r.name = :name", Role.class)
                        .setParameter("name", "PLAYER")
                        .uniqueResult();
                if (playerRole != null) newUser.setRole(playerRole);

                session.persist(newUser);
                transaction.commit();
                return true;

            } catch (Exception e) {
                // Si falla hacemos rollback para deshacer cualquier cambio a medias
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                e.printStackTrace();
                return false;
            }
        }
    }

    public OfferCreationResult createOffer(Offer offer) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                int creatorId = offer.getCreator().getId();
                User creator = session.get(User.class, creatorId);

                OfferCreationResult result;

                if (creator == null) {
                    transaction.rollback(); // Cerrar la transacción directamente y liberar recursos
                    result = OfferCreationResult.USER_NOT_FOUND;
                } else if (creator.getCurrentOffer() != null) {
                    transaction.rollback();
                    result = OfferCreationResult.ALREADY_HAS_ACTIVE_OFFER;
                } else {
                    Chat offerChat = new Chat();
                    Videogame videogame = session.get(Videogame.class, offer.getVideogame().getId());
                    if (videogame != null) offerChat.setName(videogame.getTitle());

                    offer.setChat(offerChat);
                    offer.setCreator(creator);
                    offer.setFull(false);
                    offer.addPlayer(creator);
                    offer.setCurrentPlayersCount(offer.getPlayers().size());
                    offer.getChat().addUser(creator);
                    session.persist(offer);

                    creator.setCurrentOffer(offer);
                    session.merge(creator); // Actualizamos creator con la nueva oferta

                    transaction.commit();
                    result = OfferCreationResult.SUCCESS;
                }

                return result;

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return OfferCreationResult.DATABASE_ERROR;
            }
        }
    }

    public boolean deleteOffer(Integer offerId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                Offer offer = session.get(Offer.class, offerId);

                if (offer != null) {
                    for (User player : offer.getPlayers()) {
                        player.setCurrentOffer(null);
                        session.merge(player);
                    }
                    session.remove(offer);
                    transaction.commit();
                    return true;
                } else {
                    return false;
                }

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    public OfferJoiningResult joinOffer(Integer offerId, Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            OfferJoiningResult result;

            try {
                Offer offer = session.get(Offer.class, offerId);

                if (offer == null) {
                    transaction.rollback();
                    result = OfferJoiningResult.OFFER_NOT_FOUND;
                } else if (offer.isFull()) {
                    transaction.rollback();
                    result = OfferJoiningResult.OFFER_FULL;
                } else {
                    User user = session.get(User.class, userId);

                    if (user == null) {
                        transaction.rollback();
                        result = OfferJoiningResult.USER_NOT_FOUND;
                    } else if (user.getCurrentOffer() != null) {
                        transaction.rollback();
                        result = OfferJoiningResult.ALREADY_IN_AN_OFFER;
                    } else {
                        offer.addPlayer(user);
                        offer.getChat().addUser(user);

                        offer.setCurrentPlayersCount(offer.getPlayers().size());
                        if (offer.getPlayers().size() >= offer.getTargetPlayersCount()) {
                            offer.setFull(true);
                        }

                        session.merge(offer);
                        session.merge(user);    // Porque se ha actualizado su currentOffer dentro de addPlayer()
                        transaction.commit();

                        result = OfferJoiningResult.SUCCESS;
                    }
                }

                return result;

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return OfferJoiningResult.DATABASE_ERROR;
            }
        }
    }

    public boolean leaveOffer(Integer offerId, Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                Offer offer = session.get(Offer.class, offerId);
                User user = session.get(User.class, userId);

                if (offer != null && user != null && offer.getPlayers().contains(user)) {
                    offer.removePlayer(user);

                    if (offer.getPlayers().isEmpty())
                        session.remove(offer);
                    else {
                        offer.setCurrentPlayersCount(offer.getPlayers().size());
                        if (offer.getPlayers().size() < offer.getTargetPlayersCount()) {
                            offer.setFull(false);
                        }
                        session.merge(offer);
                        session.merge(user);
                    }

                    transaction.commit();
                    return true;
                } else {
                    transaction.rollback();
                    return false;
                }

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    // De momento no se usa, solo se borra chat cuando se salen todos
//    public boolean deleteChat(Integer chatId) {
//        try (Session session = sessionFactory.openSession()) {
//            Transaction transaction = session.beginTransaction();
//
//            try {
//                Chat chat = session.get(Chat.class, chatId);
//
//                if (chat != null) {
//                    session.remove(chat);
//                    transaction.commit();
//                    return true;
//                } else {
//                    return false;
//                }
//
//            } catch (Exception e) {
//                if (transaction.isActive())
//                    transaction.rollback();
//                e.printStackTrace();
//                return false;
//            }
//        }
//    }

    public boolean leaveChat(Integer chatId, Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                Chat chat = session.get(Chat.class, chatId);
                User user = session.get(User.class, userId);

                if (chat != null && user != null && chat.getUsers().contains(user)) {
                    chat.removeUser(user);

                    // Si ya no quedan usuarios, eliminamos el chat
                    if (chat.getUsers().isEmpty()) {
                        if (chat.getOffer() != null) {
                            chat.getOffer().setChat(null);
                            session.merge(chat.getOffer());
                        }
                        session.remove(chat);
                    }
                    else {
                        session.merge(chat);
                    }

                    transaction.commit();
                    return true;
                } else {
                    transaction.rollback();
                    return false;
                }

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean updateUser(Integer userId, String newUsername, String newEmail, String newPassword) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                User user = session.get(User.class, userId);
                if (user == null) {
                    transaction.rollback();
                    return false;
                }
                if (newUsername != null) user.setUsername(newUsername);
                if (newEmail != null) user.setEmail(newEmail);
                if (newPassword != null) user.setPassword(newPassword);
                session.merge(user);
                transaction.commit();
                return true;
            } catch (Exception e) {
                if (transaction.isActive()) transaction.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    public Message registerMessage(Message message) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                Chat chat = session.get(Chat.class, message.getChat().getId());
                User user = session.get(User.class, message.getUser().getId());

                if (chat != null && user != null) {
                    // Le ponemos los objetos reales de sus atributos
                    message.setChat(chat);
                    message.setUser(user);
                    session.persist(message);

                    transaction.commit();
                } else {
                    transaction.rollback();
                }
                return message;

            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                e.printStackTrace();
                return message;
            }
        }
    }

    public String getUserRole(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null || user.getRole() == null) return "PLAYER";
            return user.getRole().getName();
        } catch (Exception e) {
            e.printStackTrace();
            return "PLAYER";
        }
    }
}