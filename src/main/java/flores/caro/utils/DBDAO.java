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
        } catch (HibernateException e) {
            System.err.println("DBDAO - getChatHistory() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getChatHistory() error: " + e.getMessage());
            return null;
        }
    }

    public Chat getOfferChat(Integer offerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Chat> query = session.createQuery("SELECT o.chat FROM Offer o WHERE o.id = :offerId", Chat.class);
            query.setParameter("offerId", offerId);
            return query.uniqueResult();
        } catch (HibernateException e) {
            System.err.println("DBDAO - getOfferChat() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getOfferChat() error: " + e.getMessage());
            return null;
        }
    }

    public List<Chat> getUserChats(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            // Hago subconsulta para obtener el último mensaje de cada chat (MAX(m.timestamp))
            // Y ordeno los chats en base a eso en orden descendente
            // Pongo null first para un chat recién creado donde no se haya hablado todavía
            Query<Chat> query = session.createQuery("FROM Chat c JOIN c.users u WHERE u.id = :userId ORDER BY (" +
                    "SELECT MAX(m.timestamp) FROM Message m WHERE m.chat = c) DESC NULLS FIRST", Chat.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (HibernateException e) {
            System.err.println("DBDAO - getUserChats() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getUserChats() error: " + e.getMessage());
            return null;
        }
    }

    public Offer getCurrentOffer(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null || user.getCurrentOffer() == null) return null;

            Query<Offer> query = session.createQuery("FROM Offer o JOIN FETCH o.videogame JOIN FETCH o.creator WHERE o.id = :offerId", Offer.class);
            query.setParameter("offerId", user.getCurrentOffer().getId());
            return query.uniqueResult();
        } catch (HibernateException e) {
            System.err.println("DBDAO - getCurrentOffer() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getCurrentOffer() error: " + e.getMessage());
            return null;
        }
    }

    public List<Videogame> getVideogames() {
        try (Session session = sessionFactory.openSession()) {
            Query<Videogame> query = session.createQuery("FROM Videogame v ORDER BY v.title", Videogame.class);
            return query.list();
        } catch (HibernateException e) {
            System.err.println("DBDAO - getVideogames() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getVideogames() error: " + e.getMessage());
            return null;
        }
    }

    public List<Offer> getFilteredOffers(Integer userId, Integer videogameId, String videogameCategory, String offerMaxTargetPlayers, String offerMinCurrentPlayers) {
        try (Session session = sessionFactory.openSession()) {

            StringBuilder querySB = new StringBuilder("FROM Offer o WHERE o.isFull = false");
            Map<String, Object> parameters = new HashMap<>();

            // Que no se muestre la oferta actual del usuario, tanto si la ha creado como si se ha unido
            User user = session.get(User.class, userId);
            Offer userCurrentOffer = user.getCurrentOffer();
            if (userCurrentOffer != null) {
                querySB.append(" AND o.id != :userCurrentOfferId");
                parameters.put("userCurrentOfferId", user.getCurrentOffer().getId());
            }

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

            // Para que salgan las más recientes primero
            querySB.append(" ORDER BY o.id DESC");
            Query<Offer> query = session.createQuery(querySB.toString(), Offer.class);
            // Se puede hacer esto porque son exactamente los mismos parámetros en el mismo orden
            parameters.forEach(query::setParameter);
            query.setMaxResults(100);

            return query.list();

        } catch (HibernateException e) {
            System.err.println("DBDAO - getFilteredOffers() Hibernate error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("DBDAO - getFilteredOffers() error: " + e.getMessage());
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

                Query<Role> query = session.createQuery("FROM Role r WHERE r.name = :name", Role.class);
                query.setParameter("name", "PLAYER");
                Role playerRole = query.uniqueResult();
                if (playerRole != null) newUser.setRole(playerRole);

                session.persist(newUser);
                transaction.commit();
                return true;

            } catch (HibernateException e) {
                // Si falla hacemos rollback para deshacer cualquier cambio a medias
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                System.err.println("DBDAO - registerUser() Hibernate error: " + e.getMessage());
                return false;
            } catch (Exception e) {
                // Si falla hacemos rollback para deshacer cualquier cambio a medias
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                System.err.println("DBDAO - registerUser() error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - createOffer() Hibernate error: " + e.getMessage());
                return OfferCreationResult.DATABASE_ERROR;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - createOffer() error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - deleteOffer() Hibernate error: " + e.getMessage());
                return false;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - deleteOffer() error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - joinOffer() Hibernate error: " + e.getMessage());
                return OfferJoiningResult.DATABASE_ERROR;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - joinOffer() error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - leaveOffer() Hibernate error: " + e.getMessage());
                return false;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - leaveOffer() error: " + e.getMessage());
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
//                System.err.println("DAO Error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - leaveChat() Hibernate error: " + e.getMessage());
                return false;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - leaveChat() error: " + e.getMessage());
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
            } catch (HibernateException e) {
                if (transaction.isActive()) transaction.rollback();
                System.err.println("DBDAO - updateUser() Hibernate error: " + e.getMessage());
                return false;
            } catch (Exception e) {
                if (transaction.isActive()) transaction.rollback();
                System.err.println("DBDAO - updateUser() error: " + e.getMessage());
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

            } catch (HibernateException e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - registerMessage() Hibernate error: " + e.getMessage());
                return message;
            } catch (Exception e) {
                if (transaction.isActive())
                    transaction.rollback();
                System.err.println("DBDAO - registerMessage() error: " + e.getMessage());
                return message;
            }
        }
    }

    public String getUserRole(Integer userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null || user.getRole() == null) return "PLAYER";
            return user.getRole().getName();
        } catch (HibernateException e) {
            System.err.println("DBDAO - getUserRole() Hibernate error: " + e.getMessage());
            return "PLAYER";
        } catch (Exception e) {
            System.err.println("DBDAO - getUserRole() error: " + e.getMessage());
            return "PLAYER";
        }
    }
}
