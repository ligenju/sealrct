package com.my.mylibrary.bean;

/**
 * Description:
 * <p>
 * CreateTime：2021/02/20  14:05
 */
public class ItemModel {
    public String userId;
    public String name;
    public String mode;
    public long joinTime;

    public ItemModel(String name, String mode) {
        this.name = name;
        this.mode = mode;
    }

    public ItemModel() {}
}