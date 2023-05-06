package com.ting.ting.controller;

import com.ting.ting.dto.request.GroupRequest;
import com.ting.ting.dto.response.GroupResponse;
import com.ting.ting.dto.response.Response;
import com.ting.ting.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    /**
     * 모든 팀 조회
     */
    @GetMapping
    public Response<Page<GroupResponse>> suggestedGroupList(@ParameterObject Pageable pageable) {
        return Response.success(groupService.findAllGroups(pageable).map(GroupResponse::from));
    }

    /**
     * 내가 속한 팀 조회
     */
    @GetMapping("/my")
    public Response<List<GroupResponse>> myGroupList() {
        // userId를 임의로 설정 TODO: user 구현 후 수정
        return Response.success(groupService.findMyGroupList(1L).stream().map(GroupResponse::from).collect(Collectors.toUnmodifiableList()));
    }

    /**
     * 그룹 생성
     */
    @PostMapping
    public Response<GroupResponse> createGroup(@RequestBody GroupRequest request) {
        Long userId = 9L;  // userId를 임의로 설정 TODO: user 구현 후 수정
        return Response.success(GroupResponse.from(groupService.saveGroup(userId, request.toDto())));
    }

    /**
     * 같은 성별인 팀에 요청
     */
    @PostMapping("/request/{groupId}")
    public Response<Void> sendJoinRequest(@PathVariable long groupId) {
        Long userId = groupId + 1;  // userId를 임의로 설정 TODO: user 구현 후 수정

        groupService.saveJoinRequest(groupId, userId);
        return Response.success();
    }

    /**
     * 같은 성별인 팀에 했던 요청을 취소
     */
    @DeleteMapping("/request/{groupId}")
    public Response<Void> deleteJoinRequest(@PathVariable long groupId) {
        Long userId = groupId + 1;  // userId를 임의로 설정 TODO: user 구현 후 수정

        groupService.deleteJoinRequest(groupId, userId);
        return Response.success();
    }
}
