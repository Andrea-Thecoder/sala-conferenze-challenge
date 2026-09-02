package com.naxos.challenge.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.naxos.challenge.config.TransactionScope;

public interface GenericRepository<T, ID extends Serializable> {

    Optional<T> findById(ID id);

    List<T> findAll(int page, int size);

    void save (T entity, TransactionScope tx);

    T update(ID id, TransactionScope tx);

    void delete(ID id, TransactionScope tx);

    long count();
}
