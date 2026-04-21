package com.javarush.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;


@Entity
@Table(schema = "world", name = "city")
@Getter
@Setter
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    private String district;

    private Integer population;
}
