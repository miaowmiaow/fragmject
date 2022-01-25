package com.example.plugin.privacy

class PrivacyEntity implements Cloneable {

    String owner
    String name
    String desc

    PrivacyEntity() {
    }

    @Override
    protected PrivacyEntity clone() {
        try {
            return (PrivacyEntity) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}