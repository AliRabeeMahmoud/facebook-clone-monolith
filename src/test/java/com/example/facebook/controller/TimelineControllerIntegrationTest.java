package com.example.facebook.controller;

import com.example.facebook.entity.Post;
import com.example.facebook.entity.Tag;
import com.example.facebook.entity.User;
import com.example.facebook.repository.PostRepository;
import com.example.facebook.repository.TagRepository;
import com.example.facebook.repository.UserRepository;
import com.example.facebook.service.UserService;
import com.example.facebook.shared.MockResourceRepo;
import com.example.facebook.shared.WithMockAuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TimelineControllerIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer=
            new MySQLContainer<>(DockerImageName.parse("mysql:latest"));
    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    TagRepository tagRepository;

    private final String API_URL_PREFIX = "/api/v1";
    private final User USER_JOHN = MockResourceRepo.getMockUserJohn();
    private final User USER_JANE = MockResourceRepo.getMockUserJane();
    private final Post POST_ONE = MockResourceRepo.getPostOne();
    private final Post POST_TWO = MockResourceRepo.getPostTwo();

    @BeforeEach
    void setUp() {
        USER_JOHN.setPassword(passwordEncoder.encode(USER_JOHN.getPassword()));
        User userJohn = userRepository.save(USER_JOHN);

        USER_JANE.setPassword(passwordEncoder.encode(USER_JANE.getPassword()));
        User userJane = userRepository.save(USER_JANE);

        POST_ONE.setAuthor(userJohn);
        postRepository.save(POST_ONE);

        POST_TWO.setAuthor(userJane);
        postRepository.save(POST_TWO);
    }

    @AfterEach
    void tearDown() {
        tagRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockAuthUser
    void getTimelinePosts() throws Exception {
        User userJane = userRepository.findByEmail(USER_JANE.getEmail()).get();

        userService.followUser(userJane.getId());

        mockMvc.perform(get(API_URL_PREFIX + "/")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockAuthUser
    void getTimelineTags() throws Exception {
        tagRepository.save(Tag.builder().name("TagOne").tagUseCounter(1).build());
        tagRepository.save(Tag.builder().name("TagTwo").tagUseCounter(3).build());

        mockMvc.perform(get(API_URL_PREFIX + "/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.[0].name").value("TagTwo"));
    }
}