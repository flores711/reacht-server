package flores.caro.utils;

import flores.caro.model.entities.Offer;
import flores.caro.model.entities.User;
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
                    offer.setCreator(creator);
                    offer.addPlayer(creator);
                    session.persist(offer); // Guardar oferta en BD

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
                    session.remove(offer);
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

            try {
                Offer offer = session.get(Offer.class, offerId);
                User user = session.get(User.class, userId);

                OfferJoiningResult result;

                if (offer == null) {
                    transaction.rollback();
                    result = OfferJoiningResult.OFFER_NOT_FOUND;
                } else if (user == null) {
                    transaction.rollback();
                    result = OfferJoiningResult.USER_NOT_FOUND;
                } else if (user.getCurrentOffer() != null) {
                    transaction.rollback();
                    result = OfferJoiningResult.ALREADY_IN_AN_OFFER;
                } else {
                    offer.addPlayer(user);

                    session.merge(offer);
                    session.merge(user);    // Porque se ha actualizado su currentOffer dentro de addPlayer()
                    transaction.commit();

                    result = OfferJoiningResult.SUCCESS;
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


}