package com.radusclifos.questlog.entry.service;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.EntryPage;
import com.radusclifos.questlog.entry.dto.EntrySearchCriteria;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.shared.search.FilterCriteria;
import com.radusclifos.questlog.shared.search.FilterOperator;
import com.radusclifos.questlog.user.service.UserService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Service
@Transactional(readOnly = true)
public class EntryService {

    private final EntryRepository entryRepository;
    private final EntryMapper entryMapper;
    private final UserService userService;
    private final GameRepository games;

    @Autowired
    public EntryService(EntryRepository entryRepository, EntryMapper entryMapper, UserService userService, GameRepository games) {
        this.entryRepository = entryRepository;
        this.entryMapper = entryMapper;
        this.userService = userService;
        this.games = games;
    }

    public EntryPage search(EntrySearchCriteria criteria) {
        if (criteria == null || StringUtils.isBlank(criteria.getUsername()) || criteria.getUsername().length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid username is required");
        }

        entryRepository.validateCriteria(criteria);

        var userId = userService.find(criteria.getUsername()).getId();
        criteria.addFilter(new FilterCriteria("user.id", FilterOperator.EQUAL, userId));

        // Preserve each sort's priority and direction when applying the status order.
        criteria.getSorts().replaceAll(sort -> "status".equals(sort.getField())
                ? EntrySearchDefaults.statusSort(sort.getDirection())
                : sort);

        if (criteria.getSorts().isEmpty()) { // Default sorting rules for entry searches without explicit sorts
            criteria.setSorts(EntrySearchDefaults.sorts());
        }

        return entryMapper.toDtoPage(entryRepository.searchWithCriteria(criteria));
    }

    // Check if the Entry is owned by the Principal
    private Entry owned(Principal principal, Long id) {
        return entryRepository.findByIdAndUserId(id, userService.current(principal).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    public EntryDto get(Principal principal, Long id) {
        return entryMapper.toDto(owned(principal, id));
    }

    @Transactional
    public EntryDto create(Principal principal, SaveEntryRequest request) {
        var user = userService.current(principal);
        var entry = entryMapper.toEntity(request, findGame(request.getGameId()));
        entry.setUser(user);
        return entryMapper.toDto(entryRepository.save(entry));
    }

    @Transactional
    public EntryDto update(Principal principal, Long id, SaveEntryRequest request) {
        var entry = owned(principal, id);
        entry.setGame(findGame(request.getGameId()));
        entryMapper.updateEntity(request, entry);
        return entryMapper.toDto(entryRepository.save(entry));
    }

    private Game findGame(Long id) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid gameId is required");
        }
        return games.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }

    @Transactional
    public void delete(Principal principal, Long id) {
        entryRepository.delete(owned(principal, id));
    }
}
