package com.radusclifos.questlog.entry.service;

import com.radusclifos.questlog.entry.*;
import com.radusclifos.questlog.entry.dto.*;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EntryService {
    private final EntryRepository entries;
    private final EntryMapper mapper;
    private final UserService users;

    public EntryService(EntryRepository entries, EntryMapper mapper, UserService users) {
        this.entries = entries;
        this.mapper = mapper;
        this.users = users;
    }

    public List<EntryDto> mine(Principal principal) {
        return mapper.toDtoList(entries.findAllByUserIdOrderByIdAsc(users.current(principal).getId()));
    }

    public List<EntryDto> forUser(String username) {
        return mapper.toDtoList(entries.findAllByUserIdOrderByIdAsc(users.find(username).getId()));
    }

    private Entry owned(Principal principal, Long id) {
        return entries.findByIdAndUserId(id, users.current(principal).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    public EntryDto get(Principal principal, Long id) {
        return mapper.toDto(owned(principal, id));
    }

    @Transactional
    public EntryDto create(Principal principal, SaveEntryRequest request) {
        var user = users.current(principal);
        var entry = mapper.toEntity(request);
        entry.setUser(user);
        return mapper.toDto(entries.save(entry));
    }

    @Transactional
    public EntryDto update(Principal principal, Long id, SaveEntryRequest request) {
        var entry = owned(principal, id);
        mapper.updateEntity(request, entry);
        return mapper.toDto(entries.save(entry));
    }

    @Transactional
    public void delete(Principal principal, Long id) {
        entries.delete(owned(principal, id));
    }
}
