package com.radusclifos.questlog.game.controller;

import com.radusclifos.questlog.game.dto.GameDto;
import com.radusclifos.questlog.game.dto.GamePage;
import com.radusclifos.questlog.game.dto.GameSuggestion;
import com.radusclifos.questlog.game.service.GameService;
import com.radusclifos.questlog.shared.search.SearchCriteria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService games;

    @PostMapping("/search")
    public GamePage search(@Valid @RequestBody SearchCriteria criteria) {
        return games.search(criteria);
    }

    @GetMapping("/suggestions")
    public List<GameSuggestion> suggestions(@RequestParam String name,
                                            @RequestParam(defaultValue = "10") int limit) {
        return games.suggestions(name, limit);
    }

    @GetMapping("/{id}")
    public GameDto findById(@PathVariable Long id) {
        return games.findById(id);
    }

}
