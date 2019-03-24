package org.outreach.outreachfeedbackauthserver.model;

public class LoginResponseModel {
    private Boolean isUserAuthentic;

    public LoginResponseModel() {
    }

    public LoginResponseModel(Boolean isUserAuthentic) {
        this.isUserAuthentic = isUserAuthentic;
    }

    public Boolean getUserAuthentic() {
        return isUserAuthentic;
    }

    public void setUserAuthentic(Boolean userAuthentic) {
        isUserAuthentic = userAuthentic;
    }

    @Override
    public String toString() {
        return "LoginResponseModel{" +
                "isUserAuthentic=" + isUserAuthentic +
                '}';
    }
}
