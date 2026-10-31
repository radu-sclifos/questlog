package com.radusclifos.questlog.game.service;

import com.radusclifos.questlog.game.dto.GameDto;
import com.radusclifos.questlog.game.dto.GamePage;
import com.radusclifos.questlog.game.dto.GameSuggestion;
import com.radusclifos.questlog.game.mapper.GameMapper;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.shared.search.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository games;
    private final GameMapper mapper;

    @Autowired
    public GameService(GameRepository games, GameMapper mapper) {
        this.games = games;
        this.mapper = mapper;
    }

    public GamePage search(SearchCriteria criteria) {
        games.validateCriteria(criteria);
        // Map inside the transaction while lazy relations are available
        return GamePage.from(games.searchWithCriteria(criteria).map(mapper::toDto));
    }

    public List<GameSuggestion> suggestions(String name, int limit) {
        if (name.isBlank() || limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name must not be blank and limit must be at least 1");
        }
        var sort = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
        return games.findByNameContainingIgnoreCase(name.strip(), PageRequest.of(0, limit, sort));
    }

    public GameDto findById(Long id) {
        return mapper.toDto(games.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")));
    }

}
