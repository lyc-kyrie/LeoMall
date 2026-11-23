package com.leomall.wx.service;

import com.leomall.db.domain.LeomallUser;
import com.leomall.db.service.LeomallUserService;
import com.leomall.wx.dto.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UserInfoService {
    @Autowired
    private LeomallUserService userService;


    public UserInfo getInfo(Integer userId) {
        LeomallUser user = userService.findById(userId);
        Assert.state(user != null, "用户不存在");
        UserInfo userInfo = new UserInfo();
        userInfo.setNickName(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatar());
        return userInfo;
    }
}
