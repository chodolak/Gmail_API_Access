package com.chodolak.gmailfinal;

public class Email {
    private long id;
    private String subject;
    private String body;
    private String author;
    private int day;
    private int month;
    private int year;
    private int urgency;

    public Email(){}

    public Email(String subject, String body, String author, int day, int month, int year, int urgency){
        super();
        this.subject = subject;
        this.body = body;
        this.author = author;
        this.day = day;
        this.month = month;
        this.year = year;
        this.urgency = urgency;
    }
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody(){
        return body;
    }

    public void setBody(String body){
        this.body = body;
    }

    public String getAuthor(){
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public long getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getUrgency() {
        return urgency;
    }

    public void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return subject;
    }
}
