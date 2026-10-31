package com.radusclifos.questlog.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCompanyDto {

    private CompanyDto company;
    private boolean developer;
    private boolean publisher;
    private boolean porting;
    private boolean supporting;

}
