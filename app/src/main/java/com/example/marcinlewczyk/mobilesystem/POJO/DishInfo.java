package com.example.marcinlewczyk.mobilesystem.POJO;

public class DishInfo {
    private String dishName;
    private int dishQty;

    public DishInfo(String dishName, int dishQty) {
        this.dishName = dishName;
        this.dishQty = dishQty;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public int getDishQty() {
        return dishQty;
    }

    public void setDishQty(int dishQty) {
        this.dishQty = dishQty;
    }
}