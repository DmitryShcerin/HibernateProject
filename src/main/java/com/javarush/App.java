package com.javarush;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.config.AppConfig;
import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.redis.CityCountry;
import com.javarush.service.CityCountryTransformer;
import com.javarush.service.RedisService;
import io.lettuce.core.RedisClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class App
{
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper mapper= new ObjectMapper();;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;
    private final RedisService redisService;

    public App(SessionFactory sessionFactory, CityDAO cityDAO, CountryDAO countryDAO, RedisClient redisClient) {
        this.sessionFactory = sessionFactory;
        this.cityDAO = cityDAO;
        this.countryDAO = countryDAO;
        this.redisClient = redisClient;
        this.redisService = new RedisService(redisClient, mapper);
    }


    public static void main( String[] args )
    {
        SessionFactory sessionFactory = AppConfig.getSessionFactory();
        RedisClient redisClient = AppConfig.getRedisClient();
        App app = new App(sessionFactory,
                new CityDAO(sessionFactory),
                new CountryDAO(sessionFactory),
                redisClient);
        List<City> allCities = app.fetchData();
        CityCountryTransformer transformer = new CityCountryTransformer();
        List<CityCountry> preparedData = transformer.transform(allCities);
        app.redisService.pushToRedis(preparedData);
        log.info("Загружено городов {}", allCities.size());
        AppConfig.close();
    }

    private List<City> fetchData() {
        try (Session session = sessionFactory.getCurrentSession()) {
            List<City> allCities = new ArrayList<>();
            session.beginTransaction();
            List<Country> countries = countryDAO.getAllCountry();
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