package com.radusclifos.questlog.user.service;

import com.radusclifos.questlog.entry.EntryStatus;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.user.UserFavoriteGame;
import com.radusclifos.questlog.user.dto.PlayingGameDto;
import com.radusclifos.questlog.user.dto.ProfileStatisticsDto;
import com.radusclifos.questlog.user.dto.PublicProfileDto;
import com.radusclifos.questlog.user.dto.SaveFavoritesRequest;
import com.radusclifos.questlog.user.repository.UserAvatarRepository;
import com.radusclifos.questlog.user.repository.UserFavoriteGameRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.EnumMap;
import java.util.HashSet;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserService users;
    private final EntryRepository entries;
    private final GameRepository games;
    private final UserFavoriteGameRepository favorites;
    private final UserAvatarRepository avatars;
    private final EntryMapper mapper;
    private final EntityManager em;

    @Autowired
    public ProfileService(UserService users, EntryRepository entries, GameRepository games,
                          UserFavoriteGameRepository favorites, UserAvatarRepository avatars,
                          EntryMapper mapper, EntityManager em) {
        this.users = users;
        this.entries = entries;
        this.games = games;
        this.favorites = favorites;
        this.avatars = avatars;
        this.mapper = mapper;
        this.em = em;
    }

    public PublicProfileDto profile(String username) {
        var user = users.find(username);
        var totals = entries.profileTotals(user.getId());
        var statuses = new EnumMap<EntryStatus, Long>(EntryStatus.class);

        for (var status : EntryStatus.values()) {
            statuses.put(status, 0L);
        }
        for (var status : entries.profileStatuses(user.getId())) {
            statuses.put(status.getStatus(), status.getAmount());
        }

        var stats = new ProfileStatisticsDto(totals.getTotalGames(), totals.getRatedGames(),
                totals.getAverageRating(), statuses.get(EntryStatus.BEATEN) + statuses.get(EntryStatus.COMPLETED),
                totals.getLoggedPlaytime(), statuses);

        var playing = entries.findByUserIdAndStatusOrderByGameNameAscIdAsc(user.getId(), EntryStatus.PLAYING,
                PageRequest.of(0, 2)).stream().map(entry -> {
            var dto = mapper.toDto(entry);
            return new PlayingGameDto(dto.getGame(), dto.getPlatform(), dto.getPlaytime());
        }).toList();

        var selFavorites = favorites.findByUserIdOrderByPosition(user.getId()).stream()
                .map(f -> mapper.toEntryGameDto(f.getGame())).toList();

        return new PublicProfileDto(user.getUsername(), user.getDisplayName(), user.getBio(), user.getCreatedAt(),
                avatars.findRevisionByUserId(user.getId()), stats, playing, selFavorites);
    }

    @Transactional
    public void saveFavorites(Principal principal, SaveFavoritesRequest request) {
        var user = users.current(principal);
        var ids = request.gameIds();
        if (ids == null || ids.size() > 5 || new HashSet<>(ids).size() != ids.size()
                || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose up to five different games");
        }

        var selected = games.findAllById(ids);
        if (selected.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more games do not exist");
        }

        // Serialize replacements for the same user
        // Lock on the user to be sure there's something to lock (in case the list is initially empty)
        em.lock(user, LockModeType.PESSIMISTIC_WRITE);
        favorites.deleteForUser(user.getId());
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            var game = selected.stream().filter(g -> g.getId().equals(id)).findFirst().orElseThrow();
            favorites.save(new UserFavoriteGame(user, game, i));
        }
    }

}
