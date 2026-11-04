package com.radusclifos.questlog.user;

import com.radusclifos.questlog.entry.*;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.user.dto.SaveFavoritesRequest;
import com.radusclifos.questlog.user.repository.*;
import com.radusclifos.questlog.user.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiles;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "questlog.catalog-import.on-startup=false", "questlog.catalog-import.scheduled=false"
})
@Transactional
class ProfileIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired UserAvatarRepository avatars;
    @Autowired ProfileService profiles;
    @Autowired EntityManager em;
    MockMvc mvc;
    User owner;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = users.save(new User("profile-owner", "Owner", "hash"));
        users.save(new User("profile-other", "Other", "hash"));
    }

    @Test void publicProfileAggregatesTheWholeLibraryAndExcludesOtherUsersAndPrivateNotes() throws Exception {
        for (int i = 0; i < 30; i++) {
            var entry = entry(owner, "Game %02d".formatted(i));
            entry.setStatus(i < 3 ? EntryStatus.PLAYING : i < 5 ? EntryStatus.BEATEN : EntryStatus.COMPLETED);
            if (i == 0) entry.setScore(0.0);
            if (i == 1) entry.setScore(10.0);
            entry.setPlaytime(i < 2 ? 12 : null);
            entry.setNotes("Do not include private notes in the profile payload");
        }
        var other = entry(users.findByUsername("profile-other").orElseThrow(), "Foreign game");
        other.setScore(10.0);
        other.setPlaytime(999);
        em.flush(); em.clear();
        mvc.perform(get("/users/profile-owner/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics.totalGames").value(30))
                .andExpect(jsonPath("$.statistics.ratedGames").value(2))
                .andExpect(jsonPath("$.statistics.averageRating").value(5.0))
                .andExpect(jsonPath("$.statistics.loggedPlaytime").value(24))
                .andExpect(jsonPath("$.statistics.finishedGames").value(27))
                .andExpect(jsonPath("$.statistics.statuses.PLAYING").value(3))
                .andExpect(jsonPath("$.statistics.statuses.DROPPED").value(0))
                .andExpect(jsonPath("$.currentlyPlaying.length()").value(2))
                .andExpect(jsonPath("$.currentlyPlaying[0].game.name").value("Game 00"))
                .andExpect(jsonPath("$.currentlyPlaying[0].notes").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test void emptyAndMissingProfilesHaveDefinedResponses() throws Exception {
        mvc.perform(get("/users/profile-owner/profile"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statistics.totalGames").value(0))
                .andExpect(jsonPath("$.statistics.averageRating").isEmpty())
                .andExpect(jsonPath("$.statistics.loggedPlaytime").isEmpty())
                .andExpect(jsonPath("$.favorites").isEmpty());
        mvc.perform(get("/users/missing/profile")).andExpect(status().isNotFound());
        owner.disable();
        mvc.perform(get("/users/profile-owner/profile")).andExpect(status().isNotFound());
    }

    @Test void favoritesAreLimitedUniqueOrderedAndReplaceableWithoutChangingAnotherUser() throws Exception {
        var games = java.util.stream.IntStream.range(0, 6).mapToObj(i -> {
            var game = new Game("Favorite " + i, "cover" + i); em.persist(game); return game;
        }).toList();
        var firstFive = games.subList(0, 5).stream().map(Game::getId).toList();
        profiles.saveFavorites(() -> "profile-owner", new SaveFavoritesRequest(firstFive));
        em.flush(); em.clear();
        assertEquals(firstFive, profiles.profile("profile-owner").getFavorites().stream().map(g -> g.getId()).toList());
        assertTrue(profiles.profile("profile-other").getFavorites().isEmpty());
        var reverse = firstFive.reversed();
        profiles.saveFavorites(() -> "profile-owner", new SaveFavoritesRequest(reverse));
        em.flush(); em.clear();
        assertEquals(reverse, profiles.profile("profile-owner").getFavorites().stream().map(g -> g.getId()).toList());
        String id = firstFive.getFirst().toString();
        mvc.perform(put("/me/favorites").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[" + id + "," + id + "]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/me/favorites").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[1,2,3,4,5,6]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/me/favorites").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[999999]}"))
                .andExpect(status().isBadRequest());
        assertEquals(reverse, profiles.profile("profile-owner").getFavorites().stream().map(g -> g.getId()).toList());
        profiles.saveFavorites(() -> "profile-owner", new SaveFavoritesRequest(List.of()));
        assertTrue(profiles.profile("profile-owner").getFavorites().isEmpty());
    }

    @Test void biographyIsValidatedAndIdentityIsStable() throws Exception {
        mvc.perform(patch("/me").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\" New name \",\"bio\":\" My story \",\"username\":\"other\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("profile-owner"));
        mvc.perform(get("/users/profile-owner/profile"))
                .andExpect(jsonPath("$.bio").value("My story")).andExpect(jsonPath("$.displayName").value("New name"));
        mvc.perform(patch("/me").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Name\",\"bio\":\"" + "x".repeat(301) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void avatarIsNormalizedStoredSeparatelyPubliclyReadableAndCacheRevalidated() throws Exception {
        var source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(source, "png", bytes);
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/me/avatar")
                .file(new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray()))
                .with(user("profile-owner")).with(csrf())).andExpect(status().isOk());
        em.flush(); em.clear();
        var result = mvc.perform(get("/users/profile-owner/avatar"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andReturn();
        var normalized = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        assertEquals(256, normalized.getWidth()); assertEquals(256, normalized.getHeight());
        var etag = result.getResponse().getHeader("ETag");
        mvc.perform(get("/users/profile-owner/avatar").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
        mvc.perform(get("/users/profile-other/avatar")).andExpect(status().isNotFound());
        mvc.perform(delete("/me/avatar").with(user("profile-other")).with(csrf())).andExpect(status().isNoContent());
        assertTrue(avatars.existsById(owner.getId()));
        mvc.perform(delete("/me/avatar").with(user("profile-owner")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/users/profile-owner/avatar")).andExpect(status().isNotFound());
    }

    @Test void malformedImagesAreRejectedWithoutReplacingExistingAvatar() throws Exception {
        avatars.saveAndFlush(new UserAvatar(owner.getId(), new byte[]{1, 2}));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/me/avatar")
                .file(new MockMultipartFile("file", "fake.png", "image/png", "<svg></svg>".getBytes()))
                .with(user("profile-owner")).with(csrf())).andExpect(status().isBadRequest());
        assertArrayEquals(new byte[]{1, 2}, avatars.findById(owner.getId()).orElseThrow().getData());
    }

    @Test void oversizedImagesAreRejectedWithoutReplacingExistingAvatar() throws Exception {
        avatars.saveAndFlush(new UserAvatar(owner.getId(), new byte[]{1, 2}));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/me/avatar")
                .file(new MockMultipartFile("file", "large.png", "image/png", new byte[2560 * 1024 + 1]))
                .with(user("profile-owner")).with(csrf())).andExpect(status().isBadRequest());
        assertArrayEquals(new byte[]{1, 2}, avatars.findById(owner.getId()).orElseThrow().getData());
    }

    @Test void profileMutationsRequireAuthenticationAndCsrf() throws Exception {
        mvc.perform(put("/me/favorites").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/me/favorites").with(user("profile-owner")).contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[]}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/me/avatar").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/me/avatar").with(user("profile-owner"))).andExpect(status().isForbidden());
        owner.disable();
        mvc.perform(put("/me/favorites").with(user("profile-owner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"gameIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    private Entry entry(User user, String name) {
        var game = new Game(name, null); em.persist(game);
        var entry = new Entry(game); entry.setUser(user); em.persist(entry); return entry;
    }
}
