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
            var repository = mock(com.radusclifos.questlog.entry.repository.EntryRepository.class);
            com.radusclifos.questlog.shared.search.SearchTestSupport.configure(repository);
            return repository;
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
        when(context.getBean(com.radusclifos.questlog.entry.repository.EntryRepository.class)
            .searchWithCriteria(any(com.radusclifos.questlog.shared.search.SearchCriteria.class)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        var encoder=context.getBean(PasswordEncoder.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(new User("alice","Alice",encoder.encode("a-long-password"))));
        org.springframework.test.util.ReflectionTestUtils.setField(users.findByUsername("alice").orElseThrow(),"id",10L);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { context.close(); }
    @Test void anonymousReadsAreDenied() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/alice/entries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/entries")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousVisitorsCanReadSelectedLibrary() throws Exception {
        var repository=context.getBean(com.radusclifos.questlog.entry.repository.EntryRepository.class);
        when(repository.searchWithCriteria(any(com.radusclifos.questlog.shared.search.SearchCriteria.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(new com.radusclifos.questlog.entry.Entry("Celeste")), org.springframework.data.domain.PageRequest.of(0,100),1));
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"alice\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].title").value("Celeste"));
    }
    @Test void selectedLibraryDoesNotDependOnViewerIdentity() throws Exception {
        mvc.perform(post("/entries/search").with(user("bob")).contentType("application/json").content("{\"username\":\"alice\"}"))
            .andExpect(status().isOk());
        verify(users,never()).findByUsername("bob");
    }
    @Test void validatesSelectedUsername() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"missing\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"\"}"))
            .andExpect(status().isBadRequest());
        var disabled=new User("disabled","Disabled","hash"); disabled.disable();
        when(users.findByUsername("disabled")).thenReturn(Optional.of(disabled));
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"disabled\"}"))
            .andExpect(status().isNotFound());
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
        verify(repository,never()).delete(any(com.radusclifos.questlog.entry.Entry.class));
    }
    @Test void usernameIsRequiredEvenForAuthenticatedViewer() throws Exception {
        mvc.perform(post("/entries/search").with(user("alice")).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void anonymousJsonSearchNeedsNoCsrfAndUsesSelectedLibrary() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"alice\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/entries/search").with(user("bob")).contentType("application/json").content("{\"username\":\"alice\"}"))
            .andExpect(status().isOk());
        verify(users,never()).findByUsername("bob");
    }
    @Test void jsonSearchDoesNotExposeOtherWriteRoutes() throws Exception {
        mvc.perform(post("/entries").contentType("application/json").content("{\"title\":\"Game\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/entries/1").contentType("application/json").content("{\"title\":\"Game\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/entries/1")).andExpect(status().isForbidden());
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"missing\"}"))
            .andExpect(status().isNotFound());
        var disabled=new User("disabled-search","Disabled","hash"); disabled.disable();
        when(users.findByUsername("disabled-search")).thenReturn(Optional.of(disabled));
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"disabled-search\"}"))
            .andExpect(status().isNotFound());
    }
    @Test void oldSearchRoutesAreRemoved() throws Exception {
        mvc.perform(get("/entries").with(user("alice"))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/users/alice/entries/search").with(user("alice")).with(csrf())
            .contentType("application/json").content("{}"))
            .andExpect(status().isNotFound());
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
