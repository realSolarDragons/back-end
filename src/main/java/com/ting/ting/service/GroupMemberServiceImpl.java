package com.ting.ting.service;

import com.ting.ting.domain.Group;
import com.ting.ting.domain.GroupMember;
import com.ting.ting.domain.GroupMemberRequest;
import com.ting.ting.domain.User;
import com.ting.ting.domain.constant.MemberRole;
import com.ting.ting.dto.response.GroupMemberRequestResponse;
import com.ting.ting.dto.response.GroupMemberResponse;
import com.ting.ting.exception.ErrorCode;
import com.ting.ting.exception.ServiceType;
import com.ting.ting.repository.GroupMemberRepository;
import com.ting.ting.repository.GroupMemberRequestRepository;
import com.ting.ting.repository.GroupRepository;
import com.ting.ting.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Component
public class GroupMemberServiceImpl extends AbstractService implements GroupMemberService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberRequestRepository groupMemberRequestRepository;

    public GroupMemberServiceImpl(UserRepository userRepository, GroupRepository groupRepository, GroupMemberRepository groupMemberRepository, GroupMemberRequestRepository groupMemberRequestRepository) {
        super(ServiceType.GROUP_MEETING);
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMemberRequestRepository = groupMemberRequestRepository;
    }

    @Override
    public void saveJoinRequest(long groupId, long userId) {
        Group group = loadGroupByGroupId(groupId);
        User user = loadUserByUserId(userId);

        if (group.getGender() != user.getGender()) {
            throwException(ErrorCode.GENDER_NOT_MATCH, String.format("Gender values of Group(id:%d) and User(id:%d) do not match", groupId, userId));
        }

        if (group.isJoinable() == false) {
            throwException(ErrorCode.REACHED_MEMBERS_SIZE_LIMIT, String.format("Maximum Group(id: %d) capacity of %d members reached", groupId, group.getMemberSizeLimit()));
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

        GroupMember memberRecordOfUser = groupMemberRepository.findByGroupAndMember(group, member).orElseThrow(() ->
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

        if (groupMemberRepository.existsByMemberAndRole(newLeader, MemberRole.LEADER)) {
            throwException(ErrorCode.DUPLICATED_REQUEST, String.format("User(id: %d) is already a leader in another group", newLeader.getId()));
        }

        GroupMember memberRecordOfLeader = groupMemberRepository.findByGroupAndMemberAndRole(group, leader, MemberRole.LEADER).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) is not the leader of Group(id: %d)", userIdOfLeader, groupId))
        );
        GroupMember memberRecordOfNewLeader = groupMemberRepository.findByGroupAndMemberAndRole(group, newLeader, MemberRole.MEMBER).orElseThrow(() ->
                throwException(ErrorCode.REQUEST_NOT_FOUND, String.format("User(id: %d) is not a member of Group(id: %d)", userIdOfNewLeader, groupId))
        );

        memberRecordOfLeader.setRole(MemberRole.MEMBER);
        memberRecordOfNewLeader.setRole(MemberRole.LEADER);

        return groupMemberRepository.saveAllAndFlush(List.of(memberRecordOfLeader, memberRecordOfNewLeader)).stream().map(GroupMemberResponse::from).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GroupMemberResponse> findGroupMemberList(Long groupId) {
        Group group = loadGroupByGroupId(groupId);

        return groupMemberRepository.findAllByGroup(group).stream().map(GroupMemberResponse::from).collect(Collectors.toUnmodifiableSet());
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

        Long actualNumOfMembers = groupMemberRepository.countByGroup(groupMemberRequest.getGroup());

        if (actualNumOfMembers >= groupMemberRequest.getGroup().getMemberSizeLimit()) {
            throwException(ErrorCode.REACHED_MEMBERS_SIZE_LIMIT, String.format("Maximum Group(id: %d) capacity of %d members reached", groupMemberRequest.getGroup().getId(), groupMemberRequest.getGroup().getMemberSizeLimit()));
        }

        throwIfUserIsNotTheLeaderOfGroup(leader, groupMemberRequest.getGroup());

        GroupMember created = groupMemberRepository.save(GroupMember.of(groupMemberRequest.getGroup(), groupMemberRequest.getUser(), MemberRole.MEMBER));
        groupMemberRequestRepository.delete(groupMemberRequest);

        if (actualNumOfMembers + 1 >= groupMemberRequest.getGroup().getMemberSizeLimit()) {
            groupMemberRequest.getGroup().setJoinable(false);
        }

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

    private void throwIfUserIsNotTheLeaderOfGroup(User leader, Group group) {
        if (!groupMemberRepository.existsByGroupAndMemberAndRole(group, leader, MemberRole.LEADER)) {
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