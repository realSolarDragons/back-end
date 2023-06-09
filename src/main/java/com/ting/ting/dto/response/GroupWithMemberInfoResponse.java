package com.ting.ting.dto.response;

import com.ting.ting.domain.Group;
import com.ting.ting.domain.constant.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@Getter
public class GroupWithMemberInfoResponse {

    private Long id;
    private String groupName;
    private Gender gender;
    private int memberSizeLimit;
    private String school;
    private boolean isMatched;
    private boolean isJoinable;
    private String memo;
    private String idealPhoto;
    private int averageAgeOfMembers;
    private Set<String> majorsOfMembers;
    private LocalDateTime createdAt;

    public static GroupWithMemberInfoResponse from(Group entity) {
        return new GroupWithMemberInfoResponse(
                entity.getId(),
                entity.getGroupName(),
                entity.getGender(),
                entity.getMemberSizeLimit(),
                entity.getSchool(),
                entity.isMatched(),
                entity.isJoinable(),
                entity.getMemo(),
                entity.getIdealPhoto(),
                entity.getAverageAgeOfMembers(),
                entity.getAllMajorsOfMembers(),
                entity.getCreatedAt()
        );
    }
}
