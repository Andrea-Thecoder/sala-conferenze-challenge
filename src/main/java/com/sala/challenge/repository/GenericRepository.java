package com.sala.challenge.repository;

import io.ebean.Transaction;

import java.io.Serializable;
import java.util.Optional;

public interface GenericRepository<T, ID extends Serializable> {

    Optional<T> findById(ID id);

    void save(T entity, Transaction tx);

    void update(T entity, Transaction tx);

    void delete(ID id, Transaction tx);
}
