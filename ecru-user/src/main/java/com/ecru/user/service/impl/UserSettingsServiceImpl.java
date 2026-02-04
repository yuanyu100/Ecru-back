package com.ecru.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecru.user.entity.UserSettings;
import com.ecru.user.mapper.UserSettingsMapper;
import com.ecru.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl extends ServiceImpl<UserSettingsMapper, UserSettings> 
        implements UserSettingsService {

    @Override
    public Map<String, String> getUserSettings(Long userId) {
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        List<UserSettings> settingsList = baseMapper.selectList(wrapper);
        
        Map<String, String> result = new HashMap<>();
        for (UserSettings setting : settingsList) {
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserSettings(Long userId, Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserSettings::getUserId, userId)
                   .eq(UserSettings::getSettingKey, entry.getKey());
            
            UserSettings existing = baseMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setSettingValue(entry.getValue());
                baseMapper.updateById(existing);
            } else {
                UserSettings newSetting = new UserSettings();
                newSetting.setUserId(userId);
                newSetting.setSettingKey(entry.getKey());
                newSetting.setSettingValue(entry.getValue());
                baseMapper.insert(newSetting);
            }
        }
    }
}
