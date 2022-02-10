package com.example.fragment.module.user.bean;

import java.util.Objects;

public class CityBean {
    private int id;
    private String name;
    private String pc_name;
    private String pinyin;
    private boolean isHot;

    public CityBean() {
    }

    public CityBean(String name, String pinyin) {
        this.name = name;
        this.pinyin = pinyin;
    }

    public CityBean(int id, String name, String pinyin) {
        this.name = name;
        this.pinyin = pinyin;
        this.id = id;
    }

    public CityBean(int id, String name, String pinyin, boolean isHot) {
        this.name = name;
        this.pinyin = pinyin;
        this.id = id;
        this.isHot = isHot;
    }

    public CityBean(int id, String name, String pc_name, String pinyin, boolean isHot) {
        this.name = name;
        this.pc_name = pc_name;
        this.pinyin = pinyin;
        this.id = id;
        this.isHot = isHot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPCName() {
        return pc_name;
    }

    public void setPCName(String pc_name) {
        this.pc_name = pc_name;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isHot() {
        return isHot;
    }

    public void setHot(boolean hot) {
        isHot = hot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CityBean cityBean = (CityBean) o;
        return Objects.equals(name, cityBean.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }
}
