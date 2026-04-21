package com.javarush.dao;

import com.javarush.domain.City;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

public class CityDAO {
    private final SessionFactory sessionFactory;
    public CityDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    public List<City> getItems(int offset, int limit) {
        Query<City> query = sessionFactory.getCurrentSession().createQuery("select c from City c join fetch c.country", City.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    public long getTotalCount() {
        Query<Long> query = sessionFactory.getCurrentSession().createQuery("select count(f) from City f", Long.class);
        return query.uniqueResult();
    }

    public City getById(Integer id) {
        Query<City> query = sessionFactory.getCurrentSession()
                .createQuery("select c from City c join fetch c.country where c.id = :ID", City.class);
        query.setParameter("ID", id);
        return query.getSingleResult();
    }
}
