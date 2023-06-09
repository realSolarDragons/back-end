package com.ting.ting.configuration;

import com.ting.ting.repository.*;
import com.ting.ting.service.*;
import com.ting.ting.util.IdealPhotoManager;
import com.ting.ting.util.JwtTokenUtil;
import com.ting.ting.util.KakaoManger;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class AppConfig {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberRequestRepository groupMemberRequestRepository;
    private final GroupDateRepository groupDateRepository;
    private final GroupDateRequestRepository groupDateRequestRepository;
    private final GroupLikeToDateRepository groupLikeToDateRepository;
    private final GroupLikeToJoinRepository groupLikeToJoinRepository;
    private final BlindRequestRepository blindRequestRepository;
    private final BlindDateRepository blindDateRepository;
    private final BlindLikeRepository blindLikeRepository;
    private final KakaoManger kakaoManger;
    private final IdealPhotoManager idealPhotoManager;
    private final JwtTokenUtil jwtTokenUtil;

    @Bean
    public UserService userService() {
        return new UserServiceImpl(userRepository, kakaoManger, jwtTokenUtil);
    }

    @Bean
    public GroupService groupService() {
        return new GroupServiceImpl(userRepository, groupRepository, groupMemberRepository, groupMemberRequestRepository, groupLikeToJoinRepository, groupLikeToDateRepository);
    }

    @Bean
    public GroupMemberService groupMemberService() {
        return new GroupMemberServiceImpl(userRepository, groupRepository, groupLikeToJoinRepository, groupMemberRepository, groupMemberRequestRepository, idealPhotoManager);
    }

    @Bean
    public GroupDateService groupDateService() {
        return new GroupDateServiceImpl(userRepository, groupRepository, groupMemberRepository, groupLikeToDateRepository, groupDateRepository, groupDateRequestRepository);
    }

    @Bean
    public GroupLikeService groupLikeService() {
        return new GroupLikeServiceImpl(userRepository, groupRepository, groupMemberRepository, groupMemberRequestRepository, groupDateRequestRepository, groupLikeToJoinRepository, groupLikeToDateRepository);
    }

    @Bean
    public BlindService blindService() {
        return new BlindServiceImpl(userRepository, blindRequestRepository, blindDateRepository, blindLikeRepository);
    }

    @Bean
    public BlindLikeService blindLikeService() {
        return new BlindLikeServiceImpl(blindLikeRepository, userRepository, blindRequestRepository, blindDateRepository);
    }
}
