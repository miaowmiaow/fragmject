package com.example.fragment.module.user.bean;

import java.util.List;

public class CityPickerBean extends BaseBean {
    public DataBean data;
    public static class DataBean {
        public List<AreasBean> areas;
    }
}
