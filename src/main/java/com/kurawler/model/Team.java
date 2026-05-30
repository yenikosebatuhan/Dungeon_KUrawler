package com.kurawler.model;

import com.kurawler.model.character.Character;
import java.util.ArrayList;
import java.util.List;

/** Stub — team match sistemi başka branch'te geliştiriliyor. */
public class Team {

    public enum TeamColor { RED, BLUE }

    private final TeamColor color;
    private final List<Character> members = new ArrayList<>();

    public Team(TeamColor color) { this.color = color; }

    public TeamColor getColor() { return color; }
    public List<Character> getMembers() { return members; }
    public void addMember(Character c) { members.add(c); }
}
