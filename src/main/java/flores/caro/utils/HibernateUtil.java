package flores.caro.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	private static final SessionFactory sessionFactory = buildSessionFactory();

	private static SessionFactory buildSessionFactory() {
		try {
            Configuration configuration = new Configuration();
            configuration.configure();  // Se puede poner configure("nombre_archivo") para un nombre diferente del archivo de configuración
            SessionFactory sessionFactory = configuration.buildSessionFactory(new StandardServiceRegistryBuilder().configure().build());

            return sessionFactory;
		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}
