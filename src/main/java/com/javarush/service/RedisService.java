package com.javarush.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.redis.CityCountry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RedisService {
    private final RedisClient redisClient;
    private final ObjectMapper mapper;

    public RedisService(RedisClient redisClient, ObjectMapper mapper) {
        this.redisClient = redisClient;
        this.mapper = mapper;
    }

    public void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    sync.set(String.valueOf(cityCountry.getId()), mapper.writeValueAsString(cityCountry));
                } catch (JsonProcessingException e) {
                    log.error("Ошибка сериализации города {}: {}", cityCountry.getId(), e.getMessage());
                }
            }
        }
    }
}