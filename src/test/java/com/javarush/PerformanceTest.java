package com.javarush;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.config.AppConfig;
import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.domain.CountryLanguage;
import com.javarush.redis.CityCountry;
import com.javarush.service.CityCountryTransformer;
import com.javarush.service.RedisService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class PerformanceTest {
    private static SessionFactory sessionFactory;
    private static RedisClient redisClient;
    private static CityDAO cityDAO;
    private static CountryDAO countryDAO;
    private static ObjectMapper mapper;
    private static RedisService redisService;
    private static List<Integer> testIds = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

    @BeforeAll
    static void setup() {
        sessionFactory = AppConfig.getSessionFactory();
        redisClient = AppConfig.getRedisClient();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);
        mapper = new ObjectMapper();
        redisService = new RedisService(redisClient, mapper);

        // Загружаем все города и записываем в Redis
        List<City> allCities = fetchAllCities();
        CityCountryTransformer transformer = new CityCountryTransformer();
        List<CityCountry> preparedData = transformer.transform(allCities);
        redisService.pushToRedis(preparedData);
        log.info("Загружено {} городов в Redis", allCities.size());
    }

    @AfterAll
    static void tearDown() {
        AppConfig.close();
    }

    @Test
    void testRedisPerformance() {
        long start = System.currentTimeMillis();
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : testIds) {
                String json = sync.get(String.valueOf(id));
                assertNotNull(json, "Данные для города " + id + " не найдены в Redis");
                mapper.readValue(json, CityCountry.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации JSON: {}", e.getMessage());
        }
        long duration = System.currentTimeMillis() - start;
        log.info("Redis чтение 10 городов: {} ms", duration);
    }

    @Test
    void testMySqlPerformance() {
        long start = System.currentTimeMillis();
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : testIds) {
                City city = cityDAO.getById(id);          // один запрос с JOIN FETCH country
                assertNotNull(city, "Город с id " + id + " не найден в MySQL");
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
                languages.size();
            }
            session.getTransaction().commit();
        }
        long duration = System.currentTimeMillis() - start;
        log.info("MySQL чтение 10 городов: {} ms", duration);
    }

    private static List<City> fetchAllCities() {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();

            List<Country> countries = countryDAO.getAllCountry();

            List<City> allCities = new ArrayList<>();
            long totalCount = cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(cityDAO.getItems(i, step));
            }

            session.getTransaction().commit();
            return allCities;
        }
    }
}
