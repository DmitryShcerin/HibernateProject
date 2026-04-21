package com.javarush.config;

import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.domain.CountryLanguage;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

@Slf4j
public class AppConfig {

    private static SessionFactory sessionFactory;
    private static RedisClient redisClient;

    private AppConfig() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = createSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory createSessionFactory() {
        Properties properties = new Properties();
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/world");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        properties.put(Environment.USER, dbUser);
        properties.put(Environment.PASS, dbPassword);
        //properties.put(Environment.USER, "root");
        //properties.put(Environment.PASS, "root");
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.HBM2DDL_AUTO, "validate");
        properties.put(Environment.STATEMENT_BATCH_SIZE, "100");

        return new Configuration()
                .addAnnotatedClass(Country.class)
                .addAnnotatedClass(City.class)
                .addAnnotatedClass(CountryLanguage.class)
                .addProperties(properties)
                .buildSessionFactory();
    }

    public static synchronized RedisClient getRedisClient() {
        if (redisClient == null) {
            redisClient = createRedisClient();
        }
        return redisClient;
    }

    private static RedisClient createRedisClient() {
        RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));
        try (var connection = client.connect()) {
            log.info("Connected to Redis");
        }
        return client;
    }

    public static void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            log.info("SessionFactory closed");
        }
        if (redisClient != null) {
            redisClient.shutdown();
            log.info("RedisClient shut down");
        }
    }
}