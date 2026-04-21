package com.javarush.dao;

import com.javarush.domain.Country;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

public class CountryDAO {
    public final SessionFactory  sessionFactory;
    public CountryDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAllCountry() {
        Query<Country> query = sessionFactory.getCurrentSession().createQuery("select c from Country c join fetch c.languages", Country.class);
        return query.list();
    }
}
