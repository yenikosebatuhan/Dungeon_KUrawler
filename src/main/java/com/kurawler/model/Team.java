package com.kurawler.model;

import com.kurawler.model.character.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team {

    public enum TeamColor { RED, BLUE }

    private final String name;
    private final TeamColor color;
    private final List<Character> members;

    public Team(String name, TeamColor color) {
        this.name = name;
        this.color = color;
        this.members = new ArrayList<>();
    }

    public void addMember(Character character) {
        if (character != null) {
            members.add(character);
        }
    }

    public boolean isEliminated() {
        for (Character member : members) {
            if (member.isAlive()) return false;
        }
        return true;
    }

    public int aliveCount() {
        int count = 0;
        for (Character member : members) {
            if (member.isAlive()) count++;
        }
        return count;
    }

    public List<Character> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Character> getAliveMembers() {
        List<Character> alive = new ArrayList<>();
        for (Character m : members) {
            if (m.isAlive()) alive.add(m);
        }
        return alive;
    }

    public String getName() { return name; }
    public TeamColor getColor() { return color; }
}
