package com.radusclifos.questlog.entry.controller;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/entries")
public class EntryController {

    private final EntryRepository entryRepository;
    private final EntryMapper entryMapper;

    public EntryController(EntryRepository entryRepository, EntryMapper entryMapper) {
        this.entryRepository = entryRepository;
        this.entryMapper = entryMapper;
    }

    @GetMapping
    public List<EntryDto> findAll() {
        return entryMapper.toDtoList(entryRepository.findAll());
    }

    @GetMapping("/{id}")
    public EntryDto findById(@PathVariable Long id) {
        return entryMapper.toDto(getEntry(id));
    }

    @PostMapping
    public ResponseEntity<EntryDto> create(@Valid @RequestBody SaveEntryRequest request) {
        var entry = entryRepository.save(entryMapper.toEntity(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(entry.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(entryMapper.toDto(entry));
    }

    @PutMapping("/{id}")
    @Transactional
    public EntryDto update(@PathVariable Long id, @Valid @RequestBody SaveEntryRequest request) {
        var entry = getEntry(id);
        entryMapper.updateEntity(request, entry);
        return entryMapper.toDto(entryRepository.save(entry));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        entryRepository.delete(getEntry(id));
    }

    private Entry getEntry(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

}
