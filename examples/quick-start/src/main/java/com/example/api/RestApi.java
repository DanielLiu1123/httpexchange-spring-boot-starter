package com.example.api;

import java.io.Serializable;
import java.util.Collection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * @author Freeman
 */
public interface RestApi<ID extends Serializable, T> {

    /**
     * Get object by id.
     *
     * @param id id
     * @return Found object
     */
    @GetExchange("/{id}")
    T get(@PathVariable("id") ID id);

    @PostExchange
    ID insert(@RequestBody T dto);

    @PutExchange
    int update(@RequestBody T dto);

    @DeleteExchange("/{id}")
    int delete(@PathVariable("id") ID id);

    @DeleteExchange
    int delete(@RequestBody Collection<ID> ids);
}
