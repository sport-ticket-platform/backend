package com.reservation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Match {
    private Long matchId;
    private Integer leagueId;
    private Integer sportId;
    private Integer venueId;
    private OffsetDateTime matchTime;
    private Integer hostTeamId;
    private Integer guestTeamId;
    private Boolean isPaymentOpen;
}