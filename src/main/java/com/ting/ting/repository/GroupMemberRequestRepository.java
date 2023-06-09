package com.ting.ting.repository;

import com.ting.ting.domain.Group;
import com.ting.ting.domain.GroupMemberRequest;
import com.ting.ting.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRequestRepository extends JpaRepository<GroupMemberRequest, Long> {

    List<GroupMemberRequest> findAllByUser(User user);

    Page<GroupMemberRequest> findAllByUser(User user, Pageable pageable);

    Optional<GroupMemberRequest> findByGroupAndUser(Group group, User user);

    void deleteByGroup_IdAndUser_Id(Long groupId, Long userId);

    @Query(value = "select entity from GroupMemberRequest entity join fetch entity.user where entity.group = :group")
    List<GroupMemberRequest> findByGroup(@Param("group") Group group);
}
