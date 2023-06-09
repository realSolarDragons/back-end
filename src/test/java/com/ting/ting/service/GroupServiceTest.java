package com.ting.ting.service;

import com.ting.ting.domain.Group;
import com.ting.ting.domain.GroupMember;
import com.ting.ting.domain.User;
import com.ting.ting.domain.constant.LikeStatus;
import com.ting.ting.domain.constant.MemberRole;
import com.ting.ting.dto.request.GroupCreateRequest;
import com.ting.ting.dto.response.DateableGroupResponse;
import com.ting.ting.dto.response.GroupDetailResponse;
import com.ting.ting.dto.response.GroupResponse;
import com.ting.ting.exception.ErrorCode;
import com.ting.ting.exception.TingApplicationException;
import com.ting.ting.fixture.GroupFixture;
import com.ting.ting.fixture.UserFixture;
import com.ting.ting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[과팅] 비즈니스 로직 테스트")
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @InjectMocks private GroupServiceImpl groupService;

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupMemberRequestRepository groupMemberRequestRepository;
    @Mock private GroupLikeToDateRepository groupLikeToDateRepository;
    @Mock private GroupLikeToJoinRepository groupLikeToJoinRepository;

    private User user;

    @BeforeEach
    private void setUpUser() {
        user = UserFixture.createUserById(1L);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(user.getId().toString()); // 원하는 userId 값을 반환하도록 설정
    }

    @DisplayName("모든 팀 조회 기능 테스트")
    @Disabled
    @Test
    void Given_Nothing_When_FindAllGroups_Then_ReturnsGroupResponsePage() {
        //Given
        Pageable pageable = Pageable.ofSize(20);
        given(groupRepository.findAllWithMemberCount(pageable)).willReturn(Page.empty());

        //When & Then
        assertThat(groupService.findAllGroups(pageable)).isEmpty();
    }

    @DisplayName("내가 속한 팀 상세 조회 기능 테스트")
    @Test
    void Given_Group_When_FindGroupDetail_Then_ReturnsGroupDetailResponse() {
        //Given
        Long groupId = 1L;

        Group group = GroupFixture.createGroupById(groupId);
        GroupMember memberRecordOfUser = GroupMember.of(group, user, MemberRole.MEMBER);
        ReflectionTestUtils.setField(group, "groupMembers", Set.of(memberRecordOfUser));

        given(groupRepository.findById(any())).willReturn(Optional.of(group));

        //When
        GroupDetailResponse response = groupService.findGroupDetail(groupId);

        //Then
        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(response.getGroup()).hasFieldOrPropertyWithValue("id", group.getId());
        assertThat(response.getMembers()).hasSize(1);
        assertThat(response).hasFieldOrPropertyWithValue("myRole", memberRecordOfUser.getRole());
    }

    @DisplayName("같은 성별 팀 가입을 위한 조회 기능 테스트")
    @Test
    void Given_Nothing_When_FindJoinableSameGenderGroupList_Then_ReturnsGroupWithRequestStatusResponsePage() {
        //Given
        Pageable pageable = Pageable.ofSize(20);

        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(groupRepository.findAllJoinableGroupWithMemberCountByGenderAndIsJoinableAndNotGroupMembers_Member(any(), anyBoolean(), any(), any())).willReturn(Page.empty());
        given(groupMemberRequestRepository.findAllByUser(user)).willReturn(List.of());
        given(groupLikeToJoinRepository.findAllByFromUser(user)).willReturn(List.of());

        //When
        assertThat(groupService.findJoinableSameGenderGroupList(pageable)).isEmpty();
    }

    @DisplayName("다른 성별 팀 과팅 요청을 위한 조회 기능 테스트")
    @Test
    void Given_Group_When_FindDateableOppositeGenderGroupList_Then_ReturnsGroupWithLikeStatusResponse() {
        //Given
        Long groupId = 1L;
        Pageable pageable = Pageable.ofSize(20);

        Group myGroup = GroupFixture.createGroupById(groupId);
        Group oppositeGenderGroup = GroupFixture.createGroupById(groupId + 1);
        User oppositeGenderGroupMember = UserFixture.createUserById(user.getId() + 1);
        GroupMember oppositeGenderGroupMemberRecord = GroupMember.of(oppositeGenderGroup, oppositeGenderGroupMember, MemberRole.LEADER);
        ReflectionTestUtils.setField(oppositeGenderGroup, "groupMembers", Set.of(oppositeGenderGroupMemberRecord));

        given(groupRepository.findById(any())).willReturn(Optional.of(myGroup));
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(groupMemberRepository.findByGroupAndMember(any(), any())).willReturn(Optional.of(mock(GroupMember.class)));
        given(groupRepository.findAllByGenderAndIsJoinableAndIsMatchedAndMemberSizeLimit(myGroup.getGender().getOpposite(), false, false, myGroup.getMemberSizeLimit(), pageable))
                .willReturn(new PageImpl<>(List.of(oppositeGenderGroup)));
        given(groupRepository.findAllWithMembersInfoByIdIn(List.of(oppositeGenderGroup.getId()))).willReturn(List.of(oppositeGenderGroup));
        given(groupLikeToDateRepository.findAllByFromGroupMember(any())).willReturn(List.of());

        //When
        Page<DateableGroupResponse> created = groupService.findDateableOppositeGenderGroupList(groupId, pageable);

        //Then
        List<DateableGroupResponse> createdList = created.getContent().stream().collect(Collectors.toList());
        assertThat(createdList).hasSize(1);
        assertThat(createdList.get(0)).hasNoNullFieldsOrPropertiesExcept("requestStatus", "likeCount", "groupDateRequestId");
        assertThat(createdList.get(0)).hasFieldOrPropertyWithValue("likeStatus", LikeStatus.NOT_LIKED);
        assertThat(createdList.get(0).getGroup().getMajorsOfMembers()).hasSize(1);
    }

    @DisplayName("내가 속한 팀 조회 기능 테스트")
    @Test
    void Given_Nothing_When_FindMyGroupList_Then_ReturnsGroupSet() {
        //Given
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(groupMemberRepository.findGroupWithMemberCountAndRoleByMember(user)).willReturn(List.of());

        //When & Then
        assertThat(groupService.findMyGroupList()).hasSize(0);
    }

    @DisplayName("팀 생성 기능 테스트")
    @Test
    void Given_GroupRequest_When_SaveGroup_Then_ReturnsCreatedGroup() {
        //Given
        GroupCreateRequest request = GroupFixture.request();
        ReflectionTestUtils.setField(user, "idealPhoto", "https://~");

        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(groupRepository.existsByGroupName(request.getGroupName())).willReturn(false);
        given(groupRepository.save(any())).willReturn(request.toEntity(user.getGender(), user.getSchool()));

        //When
        GroupResponse actual = groupService.saveGroup(request);

        //Then
        assertThat(actual.getGroupName()).isSameAs(request.getGroupName());
        then(groupMemberRepository).should().save(any(GroupMember.class));
    }

    @DisplayName("팀 생성 기능 테스트 - 생성하는 멤버의 idealPhoto 가 null 인 경우")
    @Test
    void Given_GroupRequestAndUserWithOutIdealPhoto_When_SaveGroup_Then_ThrowsException() {
        //Given
        GroupCreateRequest request = GroupFixture.request();

        given(userRepository.findById(any())).willReturn(Optional.of(user));

        //When
        Throwable t = catchThrowable(() -> groupService.saveGroup(request));

        //Then
        assertThat(t)
                .isInstanceOf(TingApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IDEAL_PHOTO);
    }
}