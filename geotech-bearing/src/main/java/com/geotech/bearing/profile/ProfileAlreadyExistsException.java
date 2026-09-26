package com.geotech.bearing.profile;

/**
 * 登记同名参数档时抛出。
 */
public class ProfileAlreadyExistsException extends RuntimeException {

    private final String profileName;

    public ProfileAlreadyExistsException(String profileName) {
        super("土层参数档名已存在：'" + profileName + "'。");
        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }
}
