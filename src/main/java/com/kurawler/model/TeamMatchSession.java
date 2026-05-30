package com.kurawler.model;

import com.kurawler.model.character.Character;

/** Stub — team match sistemi ba��ka branch'te geliştiriliyor. */
public class TeamMatchSession extends GameSession {

    private final Team redTeam  = new Team(Team.TeamColor.RED);
    private final Team blueTeam = new Team(Team.TeamColor.BLUE);
    private String matchResult;

    public TeamMatchSession(Map map) { super(map); }

    public Team getTeamForCharacter(Character c) {
        if (redTeam.getMembers().contains(c))  return redTeam;
        if (blueTeam.getMembers().contains(c)) return blueTeam;
        return null;
    }

    public String getMatchResult() { return matchResult; }
    public void setMatchResult(String result) { this.matchResult = result; }
    public Team getRedTeam()  { return redTeam; }
    public Team getBlueTeam() { return blueTeam; }
}
