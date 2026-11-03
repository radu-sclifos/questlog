package com.radusclifos.questlog.entry.repository.projection;

public interface ProfileTotals {

    long getTotalGames();

    long getRatedGames();

    Double getAverageRating();

    Long getLoggedPlaytime();

}
