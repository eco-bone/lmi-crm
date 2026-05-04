package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.GroupProspectRepository;
import com.lmi.crm.dao.GroupRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddGroupRequest;
import com.lmi.crm.dto.request.UpdateGroupRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.GroupResponse;
import com.lmi.crm.dto.response.GroupsPageResponse;
import com.lmi.crm.dto.response.GroupsSummaryResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.Group;
import com.lmi.crm.entity.GroupProspect;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.GroupMapper;
import com.lmi.crm.mapper.ProspectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupProspectRepository groupProspectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private ProspectMapper prospectMapper;

    @Override
    @Transactional
    public GroupResponse addGroup(AddGroupRequest request, Integer requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new RuntimeException("Access denied");
        }

        Integer effectiveLicenseeId;
        if (requestingUser.getRole() == UserRole.LICENSEE) {
            effectiveLicenseeId = requestingUserId;
        } else {
            effectiveLicenseeId = requestingUser.getLicenseeId();
            if (effectiveLicenseeId == null) {
                throw new RuntimeException("Associate is not linked to a licensee");
            }
        }

        Integer effectiveFacilitatorId;
        if (request.getFacilitatorId() != null) {
            User facilitator = userRepository.findById(request.getFacilitatorId())
                    .orElseThrow(() -> new RuntimeException("Facilitator not found"));
            if (facilitator.getRole() != UserRole.LICENSEE) {
                throw new RuntimeException("Facilitator must be a Licensee");
            }
            effectiveFacilitatorId = request.getFacilitatorId();
        } else {
            effectiveFacilitatorId = effectiveLicenseeId;
        }

        for (Integer prospectId : request.getProspectIds()) {
            Prospect prospect = prospectRepository.findById(prospectId)
                    .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                    .orElseThrow(() -> new RuntimeException("Prospect not found: " + prospectId));
            if (prospect.getType() != ProspectType.CLIENT) {
                throw new RuntimeException("Prospect " + prospectId + " is not a client");
            }
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, effectiveLicenseeId)) {
                throw new RuntimeException("Prospect " + prospectId + " does not belong to your licensee");
            }
        }

        Group group = groupMapper.fromAddGroupRequest(request, effectiveLicenseeId, effectiveFacilitatorId, requestingUserId);
        Group savedGroup = groupRepository.save(group);

        for (Integer prospectId : request.getProspectIds()) {
            GroupProspect gp = GroupProspect.builder()
                    .groupId(savedGroup.getId())
                    .prospectId(prospectId)
                    .build();
            groupProspectRepository.save(gp);
        }

        log.info("Group created — id: {}, licenseeId: {}, createdBy: {}", savedGroup.getId(), effectiveLicenseeId, requestingUserId);
        return buildGroupResponse(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getGroups(Integer requestingUserId, boolean getAll, Integer licenseeIdFilter, int page, int limit) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("getGroups — requestingUserId: {}, getAll: {}, licenseeIdFilter: {}, page: {}, limit: {}",
                requestingUserId, getAll, licenseeIdFilter, page, limit);

        List<Group> groups;
        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            Integer parentLicenseeId = requestingUser.getLicenseeId();
            groups = groupRepository.findByLicenseeIdAndDeletionStatusFalse(parentLicenseeId);
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            groups = groupRepository.findByLicenseeIdAndDeletionStatusFalse(requestingUserId);
        } else if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            if (licenseeIdFilter != null) {
                groups = groupRepository.findByLicenseeIdAndDeletionStatusFalse(licenseeIdFilter);
            } else {
                groups = groupRepository.findByDeletionStatusFalse();
            }
        } else {
            throw new RuntimeException("Access denied");
        }

        List<Integer> userIds = groups.stream()
                .flatMap(g -> Stream.of(g.getLicenseeId(), g.getFacilitatorId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, User> userCache = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> userCache.put(u.getId(), u));

        List<GroupResponse> allResponses = groups.stream()
                .map(g -> buildGroupResponse(g, userCache))
                .toList();

        long overallTotal = allResponses.size();
        long activeCount = overallTotal; // all fetched groups have deletionStatus = false

        if (getAll) {
            int end = Math.min(limit, allResponses.size());
            Page<GroupResponse> firstPage = new PageImpl<>(
                    end > 0 ? allResponses.subList(0, end) : List.of(),
                    PageRequest.of(0, limit),
                    allResponses.size());

            log.info("getGroups — getAll mode — requestingUserId: {}, overallTotal: {}", requestingUserId, overallTotal);

            return GroupsSummaryResponse.builder()
                    .overallTotal(overallTotal)
                    .activeCount(activeCount)
                    .firstPage(firstPage)
                    .build();
        } else {
            int start = page * limit;
            int end = Math.min(start + limit, allResponses.size());
            List<GroupResponse> pageContent = start < allResponses.size()
                    ? allResponses.subList(start, end)
                    : List.of();
            Page<GroupResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), allResponses.size());

            log.info("getGroups — paginated mode — requestingUserId: {}, overallTotal: {}, page: {}, limit: {}",
                    requestingUserId, overallTotal, page, limit);

            return GroupsPageResponse.builder()
                    .overallTotal(overallTotal)
                    .activeCount(activeCount)
                    .groups(pageResult)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetail(Integer requestingUserId, Integer groupId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (!group.getLicenseeId().equals(requestingUser.getLicenseeId())) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!group.getLicenseeId().equals(requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        log.info("GET /api/groups/{} — requestingUserId: {}", groupId, requestingUserId);
        return buildGroupResponse(group);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Integer requestingUserId, Integer groupId, UpdateGroupRequest request) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (!group.getLicenseeId().equals(requestingUser.getLicenseeId())) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!group.getLicenseeId().equals(requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        if (request.getGroupSize() != null) group.setGroupSize(request.getGroupSize());
        if (request.getGroupType() != null) group.setGroupType(request.getGroupType());
        if (request.getDeliveryType() != null) group.setDeliveryType(request.getDeliveryType());
        if (request.getStartDate() != null) group.setStartDate(request.getStartDate());
        if (request.getPpmTfeDateSent() != null) group.setPpmTfeDateSent(request.getPpmTfeDateSent());

        if (request.getFacilitatorId() != null &&
                (requestingUser.getRole() == UserRole.LICENSEE ||
                 requestingUser.getRole() == UserRole.ADMIN ||
                 requestingUser.getRole() == UserRole.SUPER_ADMIN)) {
            User facilitator = userRepository.findById(request.getFacilitatorId())
                    .orElseThrow(() -> new RuntimeException("Facilitator not found"));
            if (facilitator.getRole() != UserRole.LICENSEE) {
                throw new RuntimeException("Facilitator must be a Licensee");
            }
            group.setFacilitatorId(request.getFacilitatorId());
        }

        if (request.getLicenseeId() != null &&
                (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN)) {
            User newLicensee = userRepository.findById(request.getLicenseeId())
                    .orElseThrow(() -> new RuntimeException("Licensee not found"));
            if (newLicensee.getRole() != UserRole.LICENSEE) {
                throw new RuntimeException("Target user is not a licensee");
            }
            group.setLicenseeId(request.getLicenseeId());
        }

        if (request.getAddProspectIds() != null) {
            for (Integer prospectId : request.getAddProspectIds()) {
                Prospect prospect = prospectRepository.findById(prospectId)
                        .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                        .orElseThrow(() -> new RuntimeException("Prospect not found: " + prospectId));
                if (prospect.getType() != ProspectType.CLIENT) {
                    throw new RuntimeException("Prospect " + prospectId + " is not a client");
                }
                if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, group.getLicenseeId())) {
                    throw new RuntimeException("Prospect " + prospectId + " does not belong to this group's licensee");
                }
                if (!groupProspectRepository.existsByGroupIdAndProspectId(groupId, prospectId)) {
                    groupProspectRepository.save(GroupProspect.builder()
                            .groupId(groupId)
                            .prospectId(prospectId)
                            .build());
                }
            }
        }

        if (request.getRemoveProspectIds() != null) {
            for (Integer prospectId : request.getRemoveProspectIds()) {
                groupProspectRepository.deleteByGroupIdAndProspectId(groupId, prospectId);
            }
        }

        Group savedGroup = groupRepository.save(group);
        log.info("Group updated — id: {}, updatedBy: {}", groupId, requestingUserId);
        return buildGroupResponse(savedGroup);
    }

    @Override
    @Transactional
    public String requestGroupDeletion(Integer requestingUserId, Integer groupId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE) {
            throw new RuntimeException("Access denied");
        }

        Group group = groupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getLicenseeId().equals(requestingUserId)) {
            throw new RuntimeException("Access denied");
        }

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.GROUP_DELETION_REQUEST, groupId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    throw new RuntimeException("A deletion request for this group is already pending");
                });

        alertService.createAlert(
                AlertType.GROUP_DELETION_REQUEST,
                "Group Deletion Request — Group id: " + groupId,
                "Licensee id " + requestingUserId + " has requested deletion of group id " + groupId,
                RelatedEntityType.GROUP,
                groupId,
                requestingUserId,
                true
        );

        return "Group deletion request submitted successfully. Awaiting admin approval.";
    }

    @Override
    @Transactional
    public String deleteGroup(Integer requestingUserId, Integer groupId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        Group group = groupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.setDeletionStatus(true);
        groupRepository.save(group);

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.GROUP_DELETION_REQUEST, groupId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    a.setStatus(AlertStatus.RESOLVED);
                    alertRepository.save(a);
                });

        log.info("Group soft deleted — id: {}, deletedBy: {}", groupId, requestingUserId);
        return "Group deleted successfully";
    }

    @Override
    @Transactional
    public ApiResponse<String> approveRejectGroupDeletion(Integer requestingUserId, Integer alertId, boolean approve) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.getAlertType() != AlertType.GROUP_DELETION_REQUEST) {
            throw new RuntimeException("Alert is not a group deletion request");
        }

        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new RuntimeException("Alert is no longer pending");
        }

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Group deletion rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            return ApiResponse.rejected("Group deletion request rejected");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);
        deleteGroup(requestingUserId, alert.getRelatedEntityId());
        log.info("Group deletion approved — groupId: {}, approvedBy: {}", alert.getRelatedEntityId(), requestingUserId);
        return ApiResponse.success("Group deleted successfully", null);
    }

    private GroupResponse buildGroupResponse(Group group) {
        List<Integer> userIds = Stream.of(group.getLicenseeId(), group.getFacilitatorId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, User> userCache = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> userCache.put(u.getId(), u));
        return buildGroupResponse(group, userCache);
    }

    private GroupResponse buildGroupResponse(Group group, Map<Integer, User> userCache) {
        User licensee = userCache.get(group.getLicenseeId());
        String licenseeName = licensee != null
                ? licensee.getFirstName() + " " + licensee.getLastName() : null;

        User facilitator = userCache.get(group.getFacilitatorId());
        String facilitatorName = facilitator != null
                ? facilitator.getFirstName() + " " + facilitator.getLastName() : null;

        List<GroupProspect> groupProspects = groupProspectRepository.findByGroupId(group.getId());

        List<Integer> prospectIds = groupProspects.stream()
                .map(GroupProspect::getProspectId)
                .toList();

        Map<Integer, Integer> licenseIdMap = prospectLicenseeRepository
                .findByProspectIdInAndIsPrimaryTrue(prospectIds)
                .stream()
                .collect(Collectors.toMap(ProspectLicensee::getProspectId, ProspectLicensee::getLicenseeId));

        List<ProspectResponse> prospectResponses = groupProspects.stream()
                .map(gp -> {
                    Prospect prospect = prospectRepository.findById(gp.getProspectId()).orElse(null);
                    if (prospect == null) return null;
                    Integer licenseeId = licenseIdMap.get(prospect.getId());
                    return prospectMapper.toResponse(prospect, licenseeId, null);
                })
                .filter(Objects::nonNull)
                .toList();

        return groupMapper.toResponse(group, licenseeName, facilitatorName, prospectResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupsPageResponse searchGroups(Integer requestingUserId, String q, String scope, int page, int limit) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
        boolean scopeAll = "all".equalsIgnoreCase(scope) || isAdmin;

        List<Group> groups;
        if (scopeAll) {
            groups = groupRepository.findByDeletionStatusFalse();
        } else if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            groups = groupRepository.findByLicenseeIdAndDeletionStatusFalse(requestingUser.getLicenseeId());
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            groups = groupRepository.findByLicenseeIdAndDeletionStatusFalse(requestingUserId);
        } else {
            throw new RuntimeException("Access denied");
        }

        List<Integer> userIds = groups.stream()
                .flatMap(g -> Stream.of(g.getLicenseeId(), g.getFacilitatorId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, User> userCache = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> userCache.put(u.getId(), u));

        String keyword = q.trim().toLowerCase();
        List<GroupResponse> filtered = groups.stream()
                .map(g -> buildGroupResponse(g, userCache))
                .filter(r -> matchesGroupKeyword(r, keyword))
                .toList();

        long overallTotal = filtered.size();

        int start = page * limit;
        int end = Math.min(start + limit, filtered.size());
        List<GroupResponse> pageContent = start < filtered.size()
                ? filtered.subList(start, end) : List.of();
        Page<GroupResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), filtered.size());

        log.info("searchGroups — requestingUserId: {}, scope: {}, total: {}", requestingUserId, scope, overallTotal);

        return GroupsPageResponse.builder()
                .overallTotal(overallTotal)
                .activeCount(overallTotal)
                .groups(pageResult)
                .build();
    }

    private boolean matchesGroupKeyword(GroupResponse r, String keyword) {
        if (r.getGroupType() != null && r.getGroupType().name().toLowerCase().contains(keyword)) return true;
        if (r.getDeliveryType() != null && r.getDeliveryType().name().toLowerCase().contains(keyword)) return true;
        if (r.getLicenseeName() != null && r.getLicenseeName().toLowerCase().contains(keyword)) return true;
        if (r.getFacilitatorName() != null && r.getFacilitatorName().toLowerCase().contains(keyword)) return true;
        if (r.getProspects() != null) {
            for (ProspectResponse p : r.getProspects()) {
                if (p.getCompanyName() != null && p.getCompanyName().toLowerCase().contains(keyword)) return true;
            }
        }
        return false;
    }
}
