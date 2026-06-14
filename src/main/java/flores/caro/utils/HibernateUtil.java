package flores.caro.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	private static final SessionFactory sessionFactory = buildSessionFactory();

	private static SessionFactory buildSessionFactory() {
		try {
            Configuration configuration = new Configuration();
            configuration.configure();
            SessionFactory sessionFactory = configuration.buildSessionFactory(new StandardServiceRegistryBuilder().configure().build());

            return sessionFactory;
		} catch (Throwable e) {
			System.err.println("HibernateUtil - Initial SessionFactory creation failed: " + e.getMessage());
			throw new ExceptionInInitializerError(e);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}
