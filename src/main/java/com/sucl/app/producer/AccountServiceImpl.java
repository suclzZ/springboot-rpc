package com.sucl.app.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService{
    private static Map<String,Account> accountMap = new HashMap<>();

    static {
        accountMap.put("1",new Account("1","tom",22));
        accountMap.put("2",new Account("2","lily",20));
    }

    @Override
    public Account get(String id) {
        return accountMap.get(id);
    }

    @Override
    public List<Account> getAll() {
        return new ArrayList<>(accountMap.values());
    }

    @Override
    public Account save(Account account) {
        String id = account.getAcId();
        if(StringUtils.isEmpty(id)){
            id = UUID.randomUUID().toString();
            account.setAcId(id);
            accountMap.put(id,account);
        }else{
            if(accountMap.get(id)==null){
                accountMap.put(account.getAcId(),account);
            }else{
                log.error("账号[{}]已存在",id);
            }
        }
        return account;
    }

    @Override
    public Account update(Account account) {
        String id = account.getAcId();
        if(StringUtils.isEmpty(id)){
            log.error("账号[{}]不存在",id);
        }else{
            accountMap.put(id,account);
        }
        return account;
    }

    @Override
    public boolean delete(String id) {
        return accountMap.remove(id)!=null;
    }
}
