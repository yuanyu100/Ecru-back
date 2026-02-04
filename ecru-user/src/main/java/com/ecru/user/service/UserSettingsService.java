package com.ecru.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecru.user.entity.UserSettings;

import java.util.Map;

public interface UserSettingsService extends IService<UserSettings> {

    Map<String, String> getUserSettings(Long userId);

    void updateUserSettings(Long userId, Map<String, String> settings);
}
