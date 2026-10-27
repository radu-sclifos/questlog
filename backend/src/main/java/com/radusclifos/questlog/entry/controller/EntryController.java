package com.radusclifos.questlog.entry.controller;

import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.EntryPage;
import com.radusclifos.questlog.entry.dto.EntrySearchCriteria;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.service.EntryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/entries")
public class EntryController {

    private final EntryService entryService;

    @Autowired
    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @PostMapping("/search")
    public EntryPage search(@Valid @RequestBody EntrySearchCriteria searchCriteria) {
        return entryService.search(searchCriteria);
    }

    @GetMapping("/{id}")
    public EntryDto findById(Principal principal, @PathVariable Long id) {
        return entryService.get(principal, id);
    }

    @PostMapping
    public ResponseEntity<EntryDto> create(Principal principal, @Valid @RequestBody SaveEntryRequest request) {
        var entry = entryService.create(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @PutMapping("/{id}")
    public EntryDto update(Principal principal, @PathVariable Long id, @Valid @RequestBody SaveEntryRequest request) {
        return entryService.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        entryService.delete(principal, id);
    }

}
