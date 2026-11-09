package com.radusclifos.questlog.company;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CompanyRole {

    DEVELOPER,
    PUBLISHER,
    PORTING,
    SUPPORTING;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

}
