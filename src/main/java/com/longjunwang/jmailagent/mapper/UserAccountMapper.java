package com.longjunwang.jmailagent.mapper;

import com.longjunwang.jmailagent.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    void insert(UserAccount userAccount);

    UserAccount selectByUserNameAndPassword(@Param("username")String username, @Param("password")String password);
}
