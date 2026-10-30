package com.radusclifos.questlog.integration.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.radusclifos.questlog.game.GameRelationType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.radusclifos.questlog.game.GameRelationType.*;

/**
 * Record used for IGDB responses.
 */
public record IgdbGame(
        Long id,
        String name,
        String summary,
        String storyline,
        @JsonProperty("first_release_date") Long firstReleaseDate,
        @JsonProperty("game_type") IgdbReference gameType,
        Cover cover,
        List<IgdbReference> platforms,
        List<IgdbReference> genres,
        @JsonProperty("game_engines") List<IgdbReference> engines,
        List<IgdbReference> franchises,
        @JsonProperty("involved_companies") List<Involvement> involvedCompanies,
        @JsonProperty("version_title") String versionTitle,
        @JsonProperty("parent_game") Long parentGame,
        @JsonProperty("version_parent") Long versionParent,
        List<Long> dlcs,
        List<Long> expansions,
        @JsonProperty("standalone_expansions") List<Long> standaloneExpansions,
        List<Long> remakes,
        List<Long> remasters,
        List<Long> ports,
        @JsonProperty("expanded_games") List<Long> expandedGames,
        List<Long> forks
) {

    public record Cover(@JsonProperty("image_id") String imageId) {
    }

    public record Link(Long targetIgdbId, GameRelationType gameRelationType) {
    }

    public record Involvement(Long id, IgdbReference company, Boolean developer,
                              Boolean publisher, Boolean porting, Boolean supporting) {
        public Involvement {
            developer = Boolean.TRUE.equals(developer);
            publisher = Boolean.TRUE.equals(publisher);
            porting = Boolean.TRUE.equals(porting);
            supporting = Boolean.TRUE.equals(supporting);
        }
    }


    public Set<Link> links() {
        Set<Link> linkSet = new LinkedHashSet<>();
        add(linkSet, parentGame, PARENT_GAME);
        add(linkSet, versionParent, VERSION_OF);
        addAll(linkSet, dlcs, DLC);
        addAll(linkSet, expansions, EXPANSION);
        addAll(linkSet, standaloneExpansions, STANDALONE_EXPANSION);
        addAll(linkSet, remakes, REMAKE);
        addAll(linkSet, remasters, REMASTER);
        addAll(linkSet, ports, PORT);
        addAll(linkSet, expandedGames, EXPANDED_GAME);
        addAll(linkSet, forks, FORK);
        return linkSet;
    }

    public String exclusionReason() {
        if (isBundle()) {
            return "bundle";
        }
        if ((versionParent != null && versionParent > 0) || (versionTitle != null && !versionTitle.isBlank())) {
            return "edition";
        }
        if (gameType != null && gameType.name() != null) {
            var type = gameType.name().strip().replace('_', ' ');
            if ("mod".equalsIgnoreCase(type) || "standalone mod".equalsIgnoreCase(type)) {
                return "mod";
            }
        }
        return null;
    }

    public boolean isBundle() {
        return gameType != null && gameType.name() != null && "bundle".equalsIgnoreCase(gameType.name().strip());
    }

    private void add(Set<Link> linkSet, Long targetIgdbId, GameRelationType gameRelationType) {
        if (targetIgdbId != null && targetIgdbId > 0 && !targetIgdbId.equals(id)) {
            linkSet.add(new Link(targetIgdbId, gameRelationType));
        }
    }

    private void addAll(Set<Link> linkSet, List<Long> targetIgdbIds, GameRelationType gameRelationType) {
        if (targetIgdbIds != null) {
            targetIgdbIds.forEach(target -> add(linkSet, target, gameRelationType));
        }
    }
}
