package com.ting.ting.service;

import com.ting.ting.domain.*;
import com.ting.ting.domain.constant.Gender;
import com.ting.ting.domain.constant.MemberRole;
import com.ting.ting.domain.constant.MemberStatus;
import com.ting.ting.dto.request.GroupRequest;
import com.ting.ting.dto.response.*;
import com.ting.ting.exception.ErrorCode;
import com.ting.ting.exception.ServiceType;
import com.ting.ting.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Transactional
@Component
public class GroupServiceImpl extends AbstractService implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberRequestRepository groupMemberRequestRepository;
    private final GroupDateRepository groupDateRepository;
    private final GroupDateRequestRepository groupDateRequestRepository;
    private final UserRepository userRepository;

    public GroupServiceImpl(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository, GroupMemberRequestRepository groupMemberRequestRepository, GroupDateRepository groupDateRepository, GroupDateRequestRepository groupDateRequestRepository, UserRepository userRepository) {
        super(ServiceType.GROUP_MEETING);
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMemberRequestRepository = groupMemberRequestRepository;
        this.groupDateRepository = groupDateRepository;
        this.groupDateRequestRepository = groupDateRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<GroupResponse> findAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable).map(GroupResponse::from);
    }

    @Override
    public Page<GroupResponse> findSuggestedGroupList(Pageable pageable) {
        // TODO : 같은 성별 이면서 내가 속한 팀이 아닌 팀 조회 구현
        return null;
    }

    @Override
    public Set<GroupResponse> findMyGroupList(Long userId) {
        User member = loadUserByUserId(userId);

        return groupMemberRepository.findAllGroupByMemberAndStatus(member, MemberStatus.ACTIVE).stream().map(GroupResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GroupMemberResponse> findGroupMemberList(Long groupId) {
        Group group = loadGroupByGroupId(groupId);

        return groupMemberRepository.findAllByGroup(group).stream().map(GroupMemberResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public GroupResponse saveGroup(Long userId, GroupRequest request) {
        User leader = loadUserByUserId(userId);

        groupRepository.findByGroupName(request.getGroupName()).ifPresent(it -> {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("Group whose name is (%s) already exists", request.getGroupName()));
        });

        Group group = groupRepository.save(request.toEntity());
        groupMemberRepository.save(GroupMember.of(group, leader, MemberStatus.ACTIVE, MemberRole.LEADER));

        return GroupResponse.from(group);
    }

    @Override
    public void saveJoinRequest(long groupId, long userId) {
        Group group = loadGroupByGroupId(groupId);
        User user = loadUserByUserId(userId);

        if (group.getGender() != user.getGender()) {
            throwException(ErrorCode.GENDER_NOT_MATCH, String.format("Gender values of Group(id:%d) and User(id:%d) do not match", groupId, userId));
        }

        groupMemberRequestRepository.findByGroupAndUser(group, user).ifPresent(it -> {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("User(id:%d) already requested to join the Group(id:%d)", userId, groupId));
        });

        if (groupMemberRepository.existsByGroupAndMember(group, user)) {
            throwException(ErrorCode.ALREADY_JOINED, String.format("User(id: %d) already joined to Group(id: %d)", userId, groupId));
        }

        groupMemberRequestRepository.save(GroupMemberRequest.of(group, user));
    }

    @Override
    public void deleteJoinRequest(long groupId, long userId) {
        groupMemberRequestRepository.deleteByGroup_IdAndUser_Id(groupId, userId);
    }

    @Override
    public void deleteGroupMember(long groupId, long userId) {
        Group group = loadGroupByGroupId(groupId);
        User member = loadUserByUserId(userId);

        GroupMember memberRecordOfUser = groupMemberRepository.findByGroupAndMemberAndStatus(group, member, MemberStatus.ACTIVE).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) is not a member of the Group(id: %d)", userId, group))
        );

        // 팀에서 나가려는 유저가 그 팀의 리더라면
        if (memberRecordOfUser.getRole().equals(MemberRole.LEADER)) {
            GroupMember memberRecordOfNewLeader = loadAvailableMemberAsNewLeaderInGroup(group);
            groupMemberRepository.delete(memberRecordOfUser);
            memberRecordOfNewLeader.setRole(MemberRole.LEADER);
            return;
        }

        groupMemberRepository.delete(memberRecordOfUser);
    }

    @Override
    public Set<GroupMemberResponse> changeGroupLeader(long groupId, long userIdOfLeader, long userIdOfNewLeader) {
        if (userIdOfLeader == userIdOfNewLeader) {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("User(id: %d) is unable to transfer ownership to themselves.", userIdOfLeader));
        }

        Group group = loadGroupByGroupId(groupId);
        User leader = loadUserByUserId(userIdOfLeader);
        User newLeader = loadUserByUserId(userIdOfNewLeader);

        if (groupMemberRepository.existsByMemberAndStatusAndRole(newLeader, MemberStatus.ACTIVE, MemberRole.LEADER)) {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("User(id: %d) is already a leader in another group", newLeader.getId()));
        }

        GroupMember memberRecordOfLeader = groupMemberRepository.findByGroupAndMemberAndStatusAndRole(group, leader, MemberStatus.ACTIVE, MemberRole.LEADER).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) is not the leader of Group(id: %d)", userIdOfLeader, groupId))
        );
        GroupMember memberRecordOfNewLeader = groupMemberRepository.findByGroupAndMemberAndStatusAndRole(group, newLeader, MemberStatus.ACTIVE, MemberRole.MEMBER).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) is not a member of Group(id: %d)", userIdOfNewLeader, groupId))
        );

        memberRecordOfLeader.setRole(MemberRole.MEMBER);
        memberRecordOfNewLeader.setRole(MemberRole.LEADER);

        return groupMemberRepository.saveAllAndFlush(List.of(memberRecordOfLeader, memberRecordOfNewLeader)).stream().map(GroupMemberResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GroupMemberRequestResponse> findMemberJoinRequest(long groupId, long userIdOfLeader) {
        Group group = loadGroupByGroupId(groupId);
        User leader = loadUserByUserId(userIdOfLeader);

        throwIfUserIsNotTheLeaderOfGroup(leader, group);

        return groupMemberRequestRepository.findByGroup(group).stream().map(GroupMemberRequestResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public GroupMemberResponse acceptMemberJoinRequest(long userIdOfLeader, long groupMemberRequestId) {
        User leader = loadUserByUserId(userIdOfLeader);
        GroupMemberRequest groupMemberRequest = groupMemberRequestRepository.findById(groupMemberRequestId).orElseThrow(() ->
            throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("GroupMemberRequest(id: %d) not found", groupMemberRequestId))
        );

        if (groupMemberRepository.existsByGroupAndMember(groupMemberRequest.getGroup(), groupMemberRequest.getUser())) {
            groupMemberRequestRepository.delete(groupMemberRequest);
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("User(id: %d) is already a member of Group(id: %d)", groupMemberRequest.getUser().getId(), groupMemberRequest.getGroup().getId()));
        }

        if (groupMemberRepository.countByGroup(groupMemberRequest.getGroup()) >= groupMemberRequest.getGroup().getNumOfMember()) {
            throwException(ErrorCode.REACHED_MEMBERS_SIZE_LIMIT, String.format("Maximum Group(id: %d) capacity of %d members reached", groupMemberRequest.getGroup().getId(), groupMemberRequest.getGroup().getNumOfMember()));
        }

        throwIfUserIsNotTheLeaderOfGroup(leader, groupMemberRequest.getGroup());

        GroupMember created = groupMemberRepository.save(GroupMember.of(groupMemberRequest.getGroup(), groupMemberRequest.getUser(), MemberStatus.ACTIVE, MemberRole.MEMBER));
        groupMemberRequestRepository.delete(groupMemberRequest);
        return GroupMemberResponse.from(created);
    }

    @Override
    public void rejectMemberJoinRequest(long userIdOfLeader, long groupMemberRequestId) {
        User leader = loadUserByUserId(userIdOfLeader);
        GroupMemberRequest groupMemberRequest = groupMemberRequestRepository.findById(groupMemberRequestId).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("GroupMemberRequest(id: %d) not found", groupMemberRequestId))
        );

        throwIfUserIsNotTheLeaderOfGroup(leader, groupMemberRequest.getGroup());

        groupMemberRequestRepository.delete(groupMemberRequest);
    }

    @Override
    public Set<GroupDateRequestResponse> findAllGroupDateRequest(long groupId, long userIdOfLeader) {
        Group group = loadGroupByGroupId(groupId);
        User leader = loadUserByUserId(userIdOfLeader);

        throwIfUserIsNotTheLeaderOfGroup(leader, group);

        return groupDateRequestRepository.findByToGroup(group).stream().map(GroupDateRequestResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public GroupDateResponse acceptGroupDateRequest(long userIdOfLeader, long groupDateRequestId) {
        User leader = loadUserByUserId(userIdOfLeader);
        Group menGroup, womenGroup;
        GroupDateRequest groupDateRequest = groupDateRequestRepository.findById(groupDateRequestId).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("GroupDateRequest(id: %d) not found", groupDateRequestId))
        );

        if (groupDateRequest.getFromGroup().getGender().equals(groupDateRequest.getToGroup().getGender())) {
            groupDateRequestRepository.delete(groupDateRequest);
            throwException(ErrorCode.INVALID_REQUEST, String.format("The gender values of fromGroup(id: %d) and toGroup(id: %d) is the same", groupDateRequest.getFromGroup().getId(), groupDateRequest.getToGroup().getId()));
        }

        if (leader.getGender().equals(Gender.MEN)) {
            menGroup = groupDateRequest.getToGroup();
            womenGroup = groupDateRequest.getFromGroup();
        } else {
            menGroup = groupDateRequest.getFromGroup();
            womenGroup = groupDateRequest.getToGroup();
        }

        if (groupDateRepository.existsByMenGroupOrWomenGroup(menGroup, womenGroup)) {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("GroupDate of fromGroup(id: %d) or toGroup(id: %d) already exists", groupDateRequest.getFromGroup().getId(), groupDateRequest.getToGroup().getId()));
        }

        throwIfUserIsNotTheLeaderOfGroup(leader, groupDateRequest.getToGroup());

        menGroup.setMatched(true);
        womenGroup.setMatched(true);

        GroupDate created = groupDateRepository.save(GroupDate.of(menGroup, womenGroup));
        groupDateRequestRepository.delete(groupDateRequest);

        return GroupDateResponse.from(created);
    }

    @Override
    public void rejectGroupDateRequest(long userIdOfLeader, long groupDateRequestId) {
        User leader = loadUserByUserId(userIdOfLeader);
        GroupDateRequest groupDateRequest = groupDateRequestRepository.findById(groupDateRequestId).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("GroupDateRequest(id: %d) not found", groupDateRequestId))
        );

        throwIfUserIsNotTheLeaderOfGroup(leader, groupDateRequest.getToGroup());

        groupDateRequestRepository.delete(groupDateRequest);
    }

    private void throwIfUserIsNotTheLeaderOfGroup(User leader, Group group) {
        if (!groupMemberRepository.existsByGroupAndMemberAndStatusAndRole(group, leader, MemberStatus.ACTIVE, MemberRole.LEADER)) {
            throwException(ErrorCode.INVALID_PERMISSION, String.format("User(id: %d) is not the leader of Group(id: %d)", leader.getId(), group.getId()));
        }
    }

    private GroupMember loadAvailableMemberAsNewLeaderInGroup(Group group) {
        List<GroupMember> members = groupMemberRepository.findAvailableMemberAsALeaderInGroup(group, PageRequest.of(0, 1));
        if (members.isEmpty()) {
            throwException(ErrorCode.NO_AVAILABLE_MEMBER_AS_LEADER);
        }

        return members.get(0);
    }

    private Group loadGroupByGroupId(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("Group(id: %d) not found", groupId))
        );
    }

    private User loadUserByUserId(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) not found", userId))
        );
    }
}
