package com.fantasynations.datasource;

import com.fantasynations.datasource.dto.ExternalPlayerDto;
import com.fantasynations.datasource.dto.ExternalTeamDto;
import java.util.List;

public interface SportsDataSource {
    List<ExternalTeamDto> getTeams();
    List<ExternalPlayerDto> getPlayers();
}
