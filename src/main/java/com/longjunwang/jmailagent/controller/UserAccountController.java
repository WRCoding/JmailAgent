package com.longjunwang.jmailagent.controller;

import cn.dev33.satoken.util.SaResult;
import com.longjunwang.jmailagent.entity.UserAccount;
import com.longjunwang.jmailagent.service.UserAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class UserAccountController {

    @Autowired
    private UserAccountService userAccountService;

    @PostMapping("/login")
    public SaResult login(@RequestBody UserAccount userAccount){
        log.info("userAccount: {}", userAccount);
        return userAccountService.login(userAccount);
    }
}
