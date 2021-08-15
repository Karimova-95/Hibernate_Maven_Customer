package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        preparedData();
//        work();
//        optimisticVersionTest();
        optimisticVersioningThreadingTest();
    }

    public static void work() {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        SessionFactory factory = configuration.buildSessionFactory();

        Session session = null;

        try {
            session = factory.getCurrentSession();
            session.beginTransaction();
            Product product = session.get(Product.class, 1L);
            System.out.println(product);
            Customer customer = session.get(Customer.class, 1L);
            System.out.println(customer);
            Manufacturer manufacturer = session.get(Manufacturer.class, 1L);
            System.out.println(manufacturer);
            System.out.println("avg price " + manufacturer.getAvgProductsPrice());
            System.out.println(manufacturer.getProducts());
            session.getTransaction().commit();
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }
//    При каждом запуске получаем один и тот же набор данных, которые лежат в файле "drop-and-create.sql"
    public static void preparedData() throws IOException {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        SessionFactory factory = configuration.buildSessionFactory();

        Session session = null;

        try {
            String sql = Files.lines(Paths.get("drop-and-create.sql")).collect(Collectors.joining(" "));
            session = factory.getCurrentSession();
            session.beginTransaction();
            session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }

    public static void optimisticVersionTest() {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        SessionFactory factory = configuration.buildSessionFactory();

        Session session = null;
        try {
            session = factory.getCurrentSession();
            session.beginTransaction();
            BigItem bigItem = session.get(BigItem.class, 1L);
            session.save(bigItem);
            System.out.println(bigItem);
            bigItem.setVal(25);
            System.out.println(bigItem);
            session.save(bigItem);
            System.out.println(bigItem);
            session.getTransaction().commit();

            session = factory.getCurrentSession();
            session.beginTransaction();
            bigItem = session.get(BigItem.class, 1L);
            System.out.println(bigItem);
            session.getTransaction().commit();
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }

    public static void optimisticVersioningThreadingTest() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();

        try {
            new Thread(() -> {
                System.out.println("Thread #1 started");
                Session session = factory.getCurrentSession();
                session.beginTransaction();
                BigItem bigItem = session.get(BigItem.class, 1L);
                bigItem.setVal(100);
                uncheckableSleep(1000);
                session.save(bigItem);
                session.getTransaction().commit();
                System.out.println("Thread #1 commited");
                if (session != null) {
                    session.close();
                }
                countDownLatch.countDown();
            }).start();

            new Thread(() -> {
                System.out.println("Thread #2 started");
                Session session = factory.getCurrentSession();
                session.beginTransaction();
                BigItem bigItem = session.get(BigItem.class, 1L);
                bigItem.setVal(200);
                uncheckableSleep(3000);
                try {
                    session.save(bigItem);
                    session.getTransaction().commit();
                    System.out.println("Thread #2 commited");
                } catch (OptimisticLockException ex) {
                    session.getTransaction().rollback();
                    System.out.println("Thread #2 rollback");
                    ex.printStackTrace();
                }
                if (session != null) {
                    session.close();
                }
                countDownLatch.countDown();
            }).start();
            try {
                countDownLatch.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.out.println("END");
        } finally {
            factory.close();
        }
    }

    public static void uncheckableSleep(int ms) {
        try{
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public static void queryPessimisticLockHandle() {
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();
        Session session = null;

        try{
            session = factory.getCurrentSession();
            session.beginTransaction();
            BigDecimal totalCost = new BigDecimal(0);
            List<Product> products = session.createQuery("select p from Product  p where  p.title = :title", Product.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)  //FOR SHARE - PostgreSQL
                    .setHint("javax.persistence.lock.timeout", 5000)
                    .setParameter("title", "Sprite")
                    .getResultList();
            for (Product p: products) {
                totalCost.add(p.getPrice());
            }
            session.getTransaction().commit();
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }

    public static void queryOptimisticLockHandle() {
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();
        Session session = null;

        try{
            session = factory.getCurrentSession();
            session.beginTransaction();
            List<Product> products = session.createQuery("select p from Product  p where  p.title = :title", Product.class)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .setParameter("title", "Sprite")
                    .getResultList();
            session.getTransaction().commit();
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }

//    как обработать, если в момент коммита вылетело исключение
    public static void rollbackEx() {
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();
        Session session = null;

        try {
            session = factory.getCurrentSession();
            session.beginTransaction();
            // do something
            session.getTransaction().commit();
//            System.out.println(manufacturer.getProducts());
        } catch (Exception ex) {
            try {
                if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE ||
                session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK) {
                    session.getTransaction().rollback();
                }
            } catch (Exception rollbackException) {
                System.err.println("Rollback failed");
                rollbackException.printStackTrace();
            } throw new RuntimeException(ex);
        } finally {
            factory.close();
            if (session != null) {
                session.close();
            }
        }
    }
}
