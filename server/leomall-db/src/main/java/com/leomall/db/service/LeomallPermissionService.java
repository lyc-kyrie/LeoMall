package com.leomall.db.service;

import com.leomall.db.dao.LeomallPermissionMapper;
import com.leomall.db.dao.LeomallRoleMapper;
import com.leomall.db.domain.LeomallPermission;
import com.leomall.db.domain.LeomallPermissionExample;
import com.leomall.db.domain.LeomallRole;
import com.leomall.db.domain.LeomallRoleExample;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LeomallPermissionService {
    @Resource
    private LeomallPermissionMapper permissionMapper;

    public Set<String> queryByRoleIds(Integer[] roleIds) {
        Set<String> permissions = new HashSet<String>();
        if(roleIds.length == 0){
            return permissions;
        }

        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdIn(Arrays.asList(roleIds)).andDeletedEqualTo(false);
        List<LeomallPermission> permissionList = permissionMapper.selectByExample(example);

        for(LeomallPermission permission : permissionList){
            permissions.add(permission.getPermission());
        }

        return permissions;
    }


    public Set<String> queryByRoleId(Integer roleId) {
        Set<String> permissions = new HashSet<String>();
        if(roleId == null){
            return permissions;
        }

        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdEqualTo(roleId).andDeletedEqualTo(false);
        List<LeomallPermission> permissionList = permissionMapper.selectByExample(example);

        for(LeomallPermission permission : permissionList){
            permissions.add(permission.getPermission());
        }

        return permissions;
    }

    public Set<String> queryByRoleId(List<Integer> roleIds) {
        Set<String> permissions = new HashSet<String>();
        if(roleIds == null || roleIds.isEmpty()){
            return permissions;
        }

        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdIn(roleIds).andDeletedEqualTo(false);
        List<LeomallPermission> permissionList = permissionMapper.selectByExample(example);

        for(LeomallPermission permission : permissionList){
            permissions.add(permission.getPermission());
        }

        return permissions;
    }

    public boolean checkSuperPermission(Integer roleId) {
        if(roleId == null){
            return false;
        }

        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdEqualTo(roleId).andPermissionEqualTo("*").andDeletedEqualTo(false);
        return permissionMapper.countByExample(example) != 0;
    }

    public boolean checkSuperPermission(List<Integer> roleIds) {
        if(roleIds == null || roleIds.isEmpty()){
            return false;
        }

        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdIn(roleIds).andPermissionEqualTo("*").andDeletedEqualTo(false);
        return permissionMapper.countByExample(example) != 0;
    }

    public void deleteByRoleId(Integer roleId) {
        LeomallPermissionExample example = new LeomallPermissionExample();
        example.or().andRoleIdEqualTo(roleId).andDeletedEqualTo(false);
        permissionMapper.logicalDeleteByExample(example);
    }

    public void add(LeomallPermission leomallPermission) {
        leomallPermission.setAddTime(LocalDateTime.now());
        leomallPermission.setUpdateTime(LocalDateTime.now());
        permissionMapper.insertSelective(leomallPermission);
    }
}
