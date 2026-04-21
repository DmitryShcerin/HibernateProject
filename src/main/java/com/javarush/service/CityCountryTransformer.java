package com.javarush.service;

import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.domain.CountryLanguage;
import com.javarush.redis.CityCountry;
import com.javarush.redis.Language;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CityCountryTransformer {

    public List<CityCountry> transform(List<City> cities) {
        return cities.stream()
                .map(this::toCityCountry)
                .collect(Collectors.toList());
    }

    private CityCountry toCityCountry(City city) {
        CityCountry res = new CityCountry();
        res.setId(city.getId());
        res.setName(city.getName());
        res.setPopulation(city.getPopulation());
        res.setDistrict(city.getDistrict());

        Country country = city.getCountry();
        res.setAlternativeCountryCode(country.getCode_2());
        res.setContinent(country.getContinent());
        res.setCountryCode(country.getCode());
        res.setCountryName(country.getName());
        res.setCountryPopulation(country.getPopulation());
        res.setCountryRegion(country.getRegion());
        res.setCountrySurfaceArea(country.getSurfaceArea());

        Set<CountryLanguage> countryLanguages = country.getLanguages();
        Set<Language> languages = countryLanguages.stream()
                .map(this::toLanguage)
                .collect(Collectors.toSet());
        res.setLanguages(languages);

        return res;
    }

    private Language toLanguage(CountryLanguage cl) {
        Language language = new Language();
        language.setLanguage(cl.getLanguage());
        language.setIsOfficial(cl.getIsOfficial());
        language.setPercentage(cl.getPercentage());
        return language;
    }
}