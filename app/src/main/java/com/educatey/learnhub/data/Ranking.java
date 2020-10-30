package com.educatey.learnhub.data;

public class Ranking {
    private String name;
    private long score;

    public Ranking() {
    }

    public Ranking(String name, long score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }
}
