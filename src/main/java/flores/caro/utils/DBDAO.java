package flores.caro.utils;

import flores.caro.model.entities.Offer;
import flores.caro.model.entities.User;
import flores.caro.model.entities.Videogame;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class DBDAO {
    public void search() {
        // Volver a asegurar si abrir todo esto en cada función es lo correcto
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        Videogame videogame = new Videogame();
        videogame.setTitle("Life Is Strange");
        videogame.setBanner("ruta/a/foto");

        Offer offer = new Offer();
        offer.setVideogame(videogame);

        session.persist(videogame);
        session.persist(offer);

        try {
            transaction.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkCredentials(String username, String password) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        Query<User> query = session.createQuery("FROM User u WHERE u.username = :username AND u.password = :password", User.class);
        query.setParameter("username", username);
        query.setParameter("password", password);

        List<User> users = query.list();

        return !users.isEmpty();
    }



    static void main() {
        new DBDAO().search();
    }
}
