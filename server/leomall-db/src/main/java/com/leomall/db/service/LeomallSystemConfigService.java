package com.leomall.db.service;

import com.leomall.db.dao.LeomallSystemMapper;
import com.leomall.db.domain.LeomallSystem;
import com.leomall.db.domain.LeomallSystemExample;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeomallSystemConfigService {
    @Resource
    private LeomallSystemMapper systemMapper;

    public Map<String, String> queryAll() {
        LeomallSystemExample example = new LeomallSystemExample();
        example.or().andDeletedEqualTo(false);

        List<LeomallSystem> systemList = systemMapper.selectByExample(example);
        Map<String, String> systemConfigs = new HashMap<>();
        for (LeomallSystem item : systemList) {
            systemConfigs.put(item.getKeyName(), item.getKeyValue());
        }

        return systemConfigs;
    }

    public Map<String, String> listMail() {
        LeomallSystemExample example = new LeomallSystemExample();
        example.or().andKeyNameLike("leomall_mall_%").andDeletedEqualTo(false);
        List<LeomallSystem> systemList = systemMapper.selectByExample(example);
        Map<String, String> data = new HashMap<>();
        for(LeomallSystem system : systemList){
            data.put(system.getKeyName(), system.getKeyValue());
        }
        return data;
    }

    public Map<String, String> listWx() {
        LeomallSystemExample example = new LeomallSystemExample();
        example.or().andKeyNameLike("leomall_wx_%").andDeletedEqualTo(false);
        List<LeomallSystem> systemList = systemMapper.selectByExample(example);
        Map<String, String> data = new HashMap<>();
        for(LeomallSystem system : systemList){
            data.put(system.getKeyName(), system.getKeyValue());
        }
        return data;
    }

    public Map<String, String> listOrder() {
        LeomallSystemExample example = new LeomallSystemExample();
        example.or().andKeyNameLike("leomall_order_%").andDeletedEqualTo(false);
        List<LeomallSystem> systemList = systemMapper.selectByExample(example);
        Map<String, String> data = new HashMap<>();
        for(LeomallSystem system : systemList){
            data.put(system.getKeyName(), system.getKeyValue());
        }
        return data;
    }

    public Map<String, String> listExpress() {
        LeomallSystemExample example = new LeomallSystemExample();
        example.or().andKeyNameLike("leomall_express_%").andDeletedEqualTo(false);
        List<LeomallSystem> systemList = systemMapper.selectByExample(example);
        Map<String, String> data = new HashMap<>();
        for(LeomallSystem system : systemList){
            data.put(system.getKeyName(), system.getKeyValue());
        }
        return data;
    }

    public void updateConfig(Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            LeomallSystemExample example = new LeomallSystemExample();
            example.or().andKeyNameEqualTo(entry.getKey()).andDeletedEqualTo(false);

            LeomallSystem system = new LeomallSystem();
            system.setKeyName(entry.getKey());
            system.setKeyValue(entry.getValue());
            system.setUpdateTime(LocalDateTime.now());
            systemMapper.updateByExampleSelective(system, example);
        }

    }

    public void addConfig(String key, String value) {
        LeomallSystem system = new LeomallSystem();
        system.setKeyName(key);
        system.setKeyValue(value);
        system.setAddTime(LocalDateTime.now());
        system.setUpdateTime(LocalDateTime.now());
        systemMapper.insertSelective(system);
    }
}
