package com.sala.challenge.repository;

import io.ebean.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface GenericRepository<T, ID extends Serializable> {

    Optional<T> findById(ID id);

    List<T> findAll(int page, int size);

    void save(T entity, Transaction tx);

    void update(T entity, Transaction tx);

    void delete(ID id, Transaction tx);

    long count();
}
