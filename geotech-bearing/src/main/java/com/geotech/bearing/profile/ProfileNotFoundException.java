package com.geotech.bearing.profile;

/**
 * 点名一个未登记参数档时抛出，明确报错而不是拿默认值凑结果。
 */
public class ProfileNotFoundException extends RuntimeException {

    private final String profileName;

    public ProfileNotFoundException(String profileName) {
        super("未登记的土层参数档：'" + profileName + "'。");
        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }
}
