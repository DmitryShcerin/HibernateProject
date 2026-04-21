package com.javarush.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(schema = "world", name = "country")
@Getter
@Setter
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "code_2")
    private String code_2;

    @Column(name = "name")
    private String name;

    @Column(name = "continent")
    @Enumerated(EnumType.ORDINAL)
    private Continent continent;

    @Column(name = "region")
    private String region;

    @Column(name = "surface_area")
    private BigDecimal surfaceArea;

    @Column(name = "indep_year")
    private Short independenceYear;

    @Column(name = "population")
    private Integer population;

    @Column(name = "life_expectancy")
    private BigDecimal lifeExpectancy;

    @Column(name = "gnp")
    private BigDecimal gnp;

    @Column(name = "gnpo_id")
    private BigDecimal gnpOld;

    @Column(name = "local_name")
    private String localName;

    @Column(name = "government_form")
    private String governmentForm;

    @Column(name = "head_of_state")
    private String headOfState;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capital")
    private City city;

    @OneToMany
    @JoinColumn(name = "country_id")
    private Set<CountryLanguage> languages;

}
