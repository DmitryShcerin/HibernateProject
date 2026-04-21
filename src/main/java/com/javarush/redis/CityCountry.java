package com.javarush.redis;

import java.math.BigDecimal;
import java.util.Set;

import com.javarush.domain.Continent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CityCountry {

    private Integer id;

    private String name;

    private String district;

    private Integer population;

    private String countryCode;

    private String alternativeCountryCode;

    private String countryName;

    private Continent continent;

    private String countryRegion;

    private BigDecimal countrySurfaceArea;

    private Integer countryPopulation;

    private Set<Language> languages;
}
