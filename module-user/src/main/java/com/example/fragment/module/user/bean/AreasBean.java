package com.example.fragment.module.user.bean;

import java.util.List;

public class AreasBean {
    public int id;
    public String name;
    public int parent_id;
    public int is_hot;
    public List<ChildrenBeanX> children;

    public static class ChildrenBeanX {
        public int id;
        public String name;
        public int parent_id;
        public int is_hot;
        public List<ChildrenBean> children;

        public static class ChildrenBean {
            public int id;
            public String name;
            public int parent_id;
            public int is_hot;
        }
    }

}
