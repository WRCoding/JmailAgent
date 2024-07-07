package com.longjunwang.jmailagent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.longjunwang.jmailagent.entity.UserAccount;
import com.longjunwang.jmailagent.mapper.UserAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    public SaResult login(UserAccount userAccount) {
        UserAccount account = userAccountMapper.selectByUserNameAndPassword(userAccount.getUsername(), userAccount.getPassword());
        if (Objects.nonNull(account)) {
            StpUtil.login(account.getId());
            log.info("token : {}", StpUtil.getTokenValue());
            return SaResult.ok("登录成功");
        }
        return SaResult.error("登录失败");
    }
}
