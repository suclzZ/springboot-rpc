package com.sucl.app.producer;

import java.util.List;

/**
 * @author sucl
 * @since 2019/7/16
 */
public interface AccountService {

    Account get(String id);

    List<Account> getAll();

    Account save(Account account);

    Account update(Account account);

    boolean delete(String id);
}
