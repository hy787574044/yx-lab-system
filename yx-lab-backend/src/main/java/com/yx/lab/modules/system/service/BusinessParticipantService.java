package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.modules.system.entity.LabOrg;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabOrgMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessParticipantService {

    public static final String STAFF_ROLE_CODE = "STAFF";

    public static final String DIRECTOR_ROLE_CODE = "DIRECTOR";

    private static final String YANZHEN_WATER_PLANT_NAME = "\u6cbf\u9547\u6c34\u5382";

    private static final String NAME_SEPARATOR = "\u3001";

    private final LabOrgMapper labOrgMapper;

    private final LabUserMapper labUserMapper;

    public boolean isYanzhenWaterPlant(Long orgId) {
        if (orgId == null) {
            return false;
        }
        LabOrg org = labOrgMapper.selectById(orgId);
        return org != null && YANZHEN_WATER_PLANT_NAME.equals(StrUtil.trim(org.getOrgName()));
    }

    public List<LabUser> listSamplingCandidates(Long orgId) {
        return listParticipantCandidates(orgId);
    }

    public List<LabUser> listDetectionCandidates(Long orgId) {
        return listParticipantCandidates(orgId);
    }

    public List<Long> includeRequiredSamplers(Long orgId, List<Long> selectedIds) {
        List<Long> normalizedIds = normalizeIds(selectedIds);
        if (!isYanzhenWaterPlant(orgId)) {
            return normalizedIds;
        }
        List<Long> directorIds = listEnabledUsers(null, Collections.singletonList(DIRECTOR_ROLE_CODE)).stream()
                .map(LabUser::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        if (directorIds.isEmpty()) {
            return normalizedIds;
        }
        List<Long> merged = new ArrayList<>(normalizedIds);
        for (Long directorId : directorIds) {
            if (!merged.contains(directorId)) {
                merged.add(directorId);
            }
        }
        return merged;
    }

    public String resolveUserNames(List<Long> userIds, String fallbackNames) {
        List<Long> ids = normalizeIds(userIds);
        if (ids.isEmpty()) {
            return StrUtil.trim(fallbackNames);
        }
        Map<Long, LabUser> userMap = labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                        .in(LabUser::getId, ids))
                .stream()
                .collect(Collectors.toMap(LabUser::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
        List<String> fallbackSegments = splitNames(fallbackNames);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            LabUser user = userMap.get(id);
            String name = resolveDisplayName(user);
            if (StrUtil.isBlank(name) && i < fallbackSegments.size()) {
                name = fallbackSegments.get(i);
            }
            if (StrUtil.isBlank(name)) {
                name = String.valueOf(id);
            }
            if (StrUtil.isNotBlank(name) && !names.contains(name)) {
                names.add(name);
            }
        }
        return String.join(NAME_SEPARATOR, names);
    }

    public boolean isDetectionAssignee(LabUser user, Long orgId) {
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return false;
        }
        String roleCode = StrUtil.trim(user.getRoleCode());
        if (STAFF_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            return orgId == null || orgId.equals(user.getOrgId());
        }
        return DIRECTOR_ROLE_CODE.equalsIgnoreCase(roleCode) && isYanzhenWaterPlant(orgId);
    }

    public LabUser findDefaultDetectionAssignee(Long orgId) {
        if (orgId == null) {
            return null;
        }
        if (isYanzhenWaterPlant(orgId)) {
            LabUser director = findFirstEnabledUser(null, DIRECTOR_ROLE_CODE);
            if (director != null) {
                return director;
            }
        }
        return findFirstEnabledUser(orgId, STAFF_ROLE_CODE);
    }

    public String resolveDisplayName(LabUser user) {
        if (user == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(user.getRealName()), StrUtil.trim(user.getUsername()));
    }

    private List<LabUser> listParticipantCandidates(Long orgId) {
        if (isYanzhenWaterPlant(orgId)) {
            return mergeUsers(
                    listEnabledUsers(orgId, Collections.singletonList(STAFF_ROLE_CODE)),
                    listEnabledUsers(null, Collections.singletonList(DIRECTOR_ROLE_CODE)));
        }
        return listEnabledUsers(orgId, Collections.singletonList(STAFF_ROLE_CODE));
    }

    private List<LabUser> mergeUsers(List<LabUser> first, List<LabUser> second) {
        Map<Long, LabUser> userMap = new LinkedHashMap<>();
        for (LabUser user : first == null ? Collections.<LabUser>emptyList() : first) {
            if (user != null && user.getId() != null) {
                userMap.put(user.getId(), user);
            }
        }
        for (LabUser user : second == null ? Collections.<LabUser>emptyList() : second) {
            if (user != null && user.getId() != null) {
                userMap.putIfAbsent(user.getId(), user);
            }
        }
        List<LabUser> users = new ArrayList<>(userMap.values());
        users.sort(Comparator
                .comparingInt(this::roleOrder)
                .thenComparing(user -> StrUtil.blankToDefault(resolveDisplayName(user), "")));
        return users;
    }

    private List<LabUser> listEnabledUsers(Long orgId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<LabUser> users = labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getStatus, 1)
                .eq(orgId != null, LabUser::getOrgId, orgId)
                .in(LabUser::getRoleCode, roleCodes));
        users.sort(Comparator
                .comparingInt(this::roleOrder)
                .thenComparing(user -> StrUtil.blankToDefault(resolveDisplayName(user), "")));
        return users;
    }

    private LabUser findFirstEnabledUser(Long orgId, String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return null;
        }
        List<LabUser> users = listEnabledUsers(orgId, Collections.singletonList(roleCode));
        return users.isEmpty() ? null : users.get(0);
    }

    private int roleOrder(LabUser user) {
        String roleCode = StrUtil.trim(user == null ? null : user.getRoleCode());
        if (STAFF_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            return 0;
        }
        if (DIRECTOR_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            return 1;
        }
        return 2;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> splitNames(String value) {
        if (StrUtil.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[\\u3001,\\uFF0C]"))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
