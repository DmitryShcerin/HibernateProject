package com.javarush;

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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
@Fork(1)
public class BenchmarkTest {

    private SessionFactory sessionFactory;
    private RedisClient redisClient;
    private CityDAO cityDAO;
    private ObjectMapper objectMapper;
    private List<Integer> testIds;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(BenchmarkTest.class.getSimpleName())
                .build();
        new Runner(options).run();
    }

    @Setup(Level.Trial)
    public void setup() {
        // Инициализация Hibernate и Redis через обновлённый AppConfig
        sessionFactory = AppConfig.getSessionFactory();
        redisClient = AppConfig.getRedisClient();
        cityDAO = new CityDAO(sessionFactory);
        objectMapper = new ObjectMapper();

        // Загрузка всех городов с инициализацией связанных сущностей (страна + языки)
        List<City> allCities = fetchAllCities();
        CityCountryTransformer transformer = new CityCountryTransformer();
        List<CityCountry> preparedData = transformer.transform(allCities);

        // Сохранение данных в Redis
        RedisService redisService = new RedisService(redisClient, objectMapper);
        redisService.pushToRedis(preparedData);
        System.out.println("Подготовлено и загружено в Redis " + allCities.size() + " городов");

        // Формирование списка ID для тестирования (10 случайных или фиксированных)
        testIds = allCities.stream()
                .map(City::getId)
                .limit(10)
                .collect(Collectors.toList());
        System.out.println("Тестовые ID: " + testIds);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        AppConfig.close();
    }

    @Benchmark
    public void readFromRedis() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : testIds) {
                String json = sync.get(String.valueOf(id));
                if (json == null) {
                    throw new RuntimeException("Данные для id " + id + " не найдены в Redis");
                }
                objectMapper.readValue(json, CityCountry.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void readFromMySQL() {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : testIds) {
                City city = cityDAO.getById(id); // уже с JOIN FETCH country
                // Инициализация ленивых коллекций (языки страны)
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
                languages.size(); // принудительная загрузка
            }
            session.getTransaction().commit();
        }
    }

    private List<City> fetchAllCities() {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();

            // Инициализация стран (чтобы countryDAO.getAllCountry() загрузил языки)
            CountryDAO countryDAO = new CountryDAO(sessionFactory);
            List<Country> countries = countryDAO.getAllCountry(); // join fetch languages

            List<City> allCities = new ArrayList<>();
            long totalCount = cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(cityDAO.getItems(i, step)); // каждый city загружается с country
            }

            session.getTransaction().commit();
            return allCities;
        }
    }
}