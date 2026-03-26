package com.auction.model;

public class Art extends Item {
    private String author;
    private int yearCreation;

    public Art(String itemId, String name, String description, double startingPrice, String author, int yearCreation) {
        super(itemId, name, description, startingPrice);
        this.author = author;
        this.yearCreation = yearCreation;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getYearCreation() {
        return yearCreation;
    }

    public void setYearCreation(int yearCreation) {
        this.yearCreation = yearCreation;
    }
}
