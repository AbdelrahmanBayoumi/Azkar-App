package com.bayoumi.datamodel;

import java.util.ArrayList;

public class AbsoluteZekr {
    private int id;
    private String text;

    public AbsoluteZekr(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public static ArrayList<AbsoluteZekr> getData() {
        ArrayList<AbsoluteZekr> absoluteZekrArrayList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            absoluteZekrArrayList.add(new AbsoluteZekr(i, "ذِكْر\u200E" + " " + i));
        }
        System.out.println(absoluteZekrArrayList);
        return absoluteZekrArrayList;
    }

    @Override
    public String toString() {
        return "AbsoluteZekr{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
