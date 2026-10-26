package com.radusclifos.questlog.user;
import com.radusclifos.questlog.config.SecurityConfig;
import com.radusclifos.questlog.auth.controller.AuthController;
import com.radusclifos.questlog.user.controller.UserController;
import com.radusclifos.questlog.user.repository.UserRepository;
import com.radusclifos.questlog.user.service.UserService;
import com.radusclifos.questlog.entry.service.EntryService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SecurityTests {
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class,AuthController.class,UserController.class,UserService.class,com.radusclifos.questlog.entry.controller.EntryController.class})
    static class Config {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean com.radusclifos.questlog.entry.repository.EntryRepository entryRepository() {
            return mock(com.radusclifos.questlog.entry.repository.EntryRepository.class);
        }
        @Bean EntryService entries(com.radusclifos.questlog.entry.repository.EntryRepository repository, UserService users) {
            return new EntryService(repository, org.mapstruct.factory.Mappers.getMapper(com.radusclifos.questlog.entry.mapper.EntryMapper.class), users);
        }
    }
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    UserRepository users;
    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh(); users=context.getBean(UserRepository.class);
        var encoder=context.getBean(PasswordEncoder.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(new User("alice","Alice",encoder.encode("a-long-password"))));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { context.close(); }
    @Test void anonymousReadsAreDenied() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/alice/entries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/entries")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousVisitorsCanReadSelectedLibrary() throws Exception {
        var alice=users.findByUsername("alice").orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(alice,"id",10L);
        var repository=context.getBean(com.radusclifos.questlog.entry.repository.EntryRepository.class);
        when(repository.findAllByUserIdOrderByIdAsc(10L)).thenReturn(java.util.List.of(new com.radusclifos.questlog.entry.Entry("Celeste")));
        mvc.perform(get("/entries").param("username","alice"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Celeste"));
        verify(repository).findAllByUserIdOrderByIdAsc(10L);
    }
    @Test void selectedLibraryDoesNotDependOnViewerIdentity() throws Exception {
        mvc.perform(get("/entries").param("username","alice").with(user("bob")))
            .andExpect(status().isOk());
        verify(users,never()).findByUsername("bob");
    }
    @Test void unknownDisabledAndEmptyUsernamesReturnNotFound() throws Exception {
        mvc.perform(get("/entries").param("username","missing")).andExpect(status().isNotFound());
        mvc.perform(get("/entries").param("username","")).andExpect(status().isNotFound());
        var disabled=new User("disabled","Disabled","hash"); disabled.disable();
        when(users.findByUsername("disabled")).thenReturn(Optional.of(disabled));
        mvc.perform(get("/entries").param("username","disabled")).andExpect(status().isNotFound());
    }
    @Test void anonymousWritesAndItemReadsRemainProtected() throws Exception {
        mvc.perform(get("/entries/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/entries").with(csrf()).param("username","alice")
                .contentType("application/json").content("{\"title\":\"Celeste\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/entries/1").with(csrf()).param("username","alice")
                .contentType("application/json").content("{\"title\":\"Celeste\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/entries/1").with(csrf()).param("username","alice"))
            .andExpect(status().isUnauthorized());
    }
    @Test void usernameParameterDoesNotAllowModifyingAnotherUsersEntry() throws Exception {
        var alice=users.findByUsername("alice").orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(alice,"id",10L);
        mvc.perform(delete("/entries/99").with(user("alice")).with(csrf()).param("username","bob"))
            .andExpect(status().isNotFound());
        var repository=context.getBean(com.radusclifos.questlog.entry.repository.EntryRepository.class);
        verify(repository).findByIdAndUserId(99L,10L);
        verify(repository,never()).delete(any());
    }
    @Test void authenticatedVisitorCanReadOwnLibraryWithoutUsername() throws Exception {
        mvc.perform(get("/entries").with(user("alice"))).andExpect(status().isOk());
    }
    @Test void csrfIsRequiredForLoginAndProfileWrites() throws Exception {
        mvc.perform(post("/auth/login").param("username","alice").param("password","a-long-password")).andExpect(status().isForbidden());
        mvc.perform(patch("/me").with(user("alice")).contentType("application/json").content("{\"displayName\":\"Changed\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
        var result=mvc.perform(post("/auth/login").with(csrf()).param("username","alice").param("password","a-long-password"))
            .andExpect(status().isNoContent()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        Assertions.assertTrue(session.isInvalid());
    }
    @Test void disabledAccountsLoseExistingSessionAccess() throws Exception {
        var disabled=new User("alice","Alice","hash"); disabled.disable(); when(users.findByUsername("alice")).thenReturn(Optional.of(disabled));
        mvc.perform(get("/me").with(user("alice"))).andExpect(status().isUnauthorized());
    }
    @Test void badCredentialsAreRejected() throws Exception {
        mvc.perform(post("/auth/login").with(csrf()).param("username","alice").param("password","wrong"))
            .andExpect(status().isUnauthorized());
    }
}
