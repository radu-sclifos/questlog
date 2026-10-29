package com.radusclifos.questlog.game.controller;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.dto.CreateGameRequest;
import com.radusclifos.questlog.game.dto.GameResponse;
import com.radusclifos.questlog.game.repository.GameRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameRepository gameRepository;

    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public List<GameResponse> findAll() {
        return gameRepository.findAll().stream()
                .map(GameController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public GameResponse findById(@PathVariable Long id) {
        var game = gameRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Game not found"));

        return toResponse(game);
    }

    @PostMapping
    public ResponseEntity<GameResponse> create(@Valid @RequestBody CreateGameRequest request) {
        var game = gameRepository.save(new Game(request.title(), request.coverImageId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(game));
    }

    private static GameResponse toResponse(Game game) {
        return new GameResponse(game.getId(), game.getName(), game.getCoverImageId());
    }
}
