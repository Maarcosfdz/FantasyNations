package com.fantasynations.datasource;

import com.fantasynations.datasource.dto.ExternalPlayerDto;
import com.fantasynations.datasource.dto.ExternalTeamDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "mock", matchIfMissing = true)
public class MockSportsDataSourceImpl implements SportsDataSource {

    @Override
    public List<ExternalTeamDto> getTeams() {
        return List.of(
            new ExternalTeamDto("ESP", "Spain", "ES", null),
            new ExternalTeamDto("FRA", "France", "FR", null),
            new ExternalTeamDto("BRA", "Brazil", "BR", null),
            new ExternalTeamDto("ARG", "Argentina", "AR", null),
            new ExternalTeamDto("POR", "Portugal", "PT", null),
            new ExternalTeamDto("ENG", "England", "EN", null),
            new ExternalTeamDto("GER", "Germany", "DE", null),
            new ExternalTeamDto("NED", "Netherlands", "NL", null)
        );
    }

    @Override
    public List<ExternalPlayerDto> getPlayers() {
        return List.of(
            // Spain
            new ExternalPlayerDto("sp-001", "Alejandro Ramos", "Spain", "GK", 6000000, null),
            new ExternalPlayerDto("sp-002", "Carlos Herrera", "Spain", "DEF", 8000000, null),
            new ExternalPlayerDto("sp-003", "Miguel Torres", "Spain", "DEF", 9000000, null),
            new ExternalPlayerDto("sp-004", "Luis Moreno", "Spain", "MID", 12000000, null),
            new ExternalPlayerDto("sp-005", "Javier García", "Spain", "FWD", 15000000, null),
            // France
            new ExternalPlayerDto("fr-001", "Mathieu Bernard", "France", "GK", 7000000, null),
            new ExternalPlayerDto("fr-002", "Antoine Dubois", "France", "DEF", 10000000, null),
            new ExternalPlayerDto("fr-003", "Julien Martin", "France", "MID", 13000000, null),
            new ExternalPlayerDto("fr-004", "Thomas Laurent", "France", "FWD", 18000000, null),
            new ExternalPlayerDto("fr-005", "Nicolas Petit", "France", "FWD", 16000000, null),
            // Brazil
            new ExternalPlayerDto("br-001", "Felipe Santos", "Brazil", "GK", 8000000, null),
            new ExternalPlayerDto("br-002", "Rafael Costa", "Brazil", "DEF", 11000000, null),
            new ExternalPlayerDto("br-003", "Lucas Oliveira", "Brazil", "MID", 14000000, null),
            new ExternalPlayerDto("br-004", "Gabriel Silva", "Brazil", "FWD", 20000000, null),
            new ExternalPlayerDto("br-005", "Diego Alves", "Brazil", "FWD", 17000000, null),
            // Argentina
            new ExternalPlayerDto("ar-001", "Rodrigo Díaz", "Argentina", "GK", 7500000, null),
            new ExternalPlayerDto("ar-002", "Mateo Fernández", "Argentina", "DEF", 9500000, null),
            new ExternalPlayerDto("ar-003", "Nicolás López", "Argentina", "MID", 15000000, null),
            new ExternalPlayerDto("ar-004", "Ezequiel Martínez", "Argentina", "FWD", 22000000, null),
            new ExternalPlayerDto("ar-005", "Agustín González", "Argentina", "MID", 12000000, null),
            // Portugal
            new ExternalPlayerDto("pt-001", "João Pereira", "Portugal", "GK", 7000000, null),
            new ExternalPlayerDto("pt-002", "Tiago Sousa", "Portugal", "DEF", 10000000, null),
            new ExternalPlayerDto("pt-003", "Bruno Ferreira", "Portugal", "MID", 14000000, null),
            new ExternalPlayerDto("pt-004", "André Carvalho", "Portugal", "FWD", 19000000, null),
            new ExternalPlayerDto("pt-005", "Rui Silva", "Portugal", "FWD", 16000000, null),
            // England
            new ExternalPlayerDto("en-001", "James Walker", "England", "GK", 8000000, null),
            new ExternalPlayerDto("en-002", "Thomas Hughes", "England", "DEF", 11000000, null),
            new ExternalPlayerDto("en-003", "Harry White", "England", "MID", 13000000, null),
            new ExternalPlayerDto("en-004", "Jack Robinson", "England", "FWD", 21000000, null),
            new ExternalPlayerDto("en-005", "Oliver Taylor", "England", "FWD", 17000000, null),
            // Germany
            new ExternalPlayerDto("de-001", "Stefan Müller", "Germany", "GK", 7500000, null),
            new ExternalPlayerDto("de-002", "Klaus Wagner", "Germany", "DEF", 10000000, null),
            new ExternalPlayerDto("de-003", "Hans Becker", "Germany", "MID", 12000000, null),
            new ExternalPlayerDto("de-004", "Erik Fischer", "Germany", "FWD", 19000000, null),
            new ExternalPlayerDto("de-005", "Lars Weber", "Germany", "MID", 13000000, null)
        );
    }
}
