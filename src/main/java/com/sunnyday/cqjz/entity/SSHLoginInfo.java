package com.sunnyday.cqjz.entity;

import java.io.Serializable;

/**
 * ssh 连接对象
 *
 * @author TMW
 * @since 2020/11/17 14:32
 */
public class SSHLoginInfo implements Serializable {
    private static final long serialVersionUID = 5146885183830748105L;
    private String ipAddress;// ip 地址
    private String userName;// 登陆帐号
    private String passWord;// 登陆密码

    private SSHLoginInfo(Builder builder) {
        this.ipAddress = builder.ipAddress;
        this.userName = builder.userName;
        this.passWord = builder.passWord;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    @Override
    public String toString() {
        return "SSHLoginInfo{" +
                "ipAddress='" + ipAddress + '\'' +
                ", userName='" + userName + '\'' +
                ", passWord='" + passWord + '\'' +
                '}';
    }

    public static class Builder {
        private String ipAddress;// ip 地址
        private String userName;// 登陆帐号
        private String passWord;// 登陆密码

        public Builder setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setPassWord(String passWord) {
            this.passWord = passWord;
            return this;
        }

        public SSHLoginInfo build() {
            return new SSHLoginInfo(this);
        }
    }

}
