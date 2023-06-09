package com.ting.ting.dto.response;

import com.ting.ting.domain.GroupMemberRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GroupMemberRequestResponse {

    private Long id;
    private UserResponse user;

    public static GroupMemberRequestResponse from(GroupMemberRequest entity) {
        return new GroupMemberRequestResponse(
                entity.getId(),
                UserResponse.from(entity.getUser())
        );
    }
}
