package com.sunnyday.cqjz.entity;

/**
 * 文件配置
 *
 * @author TMW
 * @since 2020/11/19 11:04
 */
public class FileProperties {
    /**
     * 源IP
     */
    private final String srcIp;
    /**
     * 源服务器账号
     */
    private final String srcUsername;
    /**
     * 源服务器密码
     */
    private final String srcPassword;
    /**
     * 源服务器文件路径
     */
    private final String srcFileDir;
    /**
     * 拷贝的源文件前缀
     */
    private final String srcFilePrefix;
    /**
     * 拷贝的源文件名称包含时间
     */
    private final String srcFileDate;
    /**
     * 拷贝的源文件类型
     */
    private final String srcFileType;
    /**
     * 目标服务器文件存放路径
     */
    private final String destFileDir;
    /**
     * 目标服务器文件解压路径
     */
    private final String unzipPath;

    private FileProperties(Builder builder) {
        this.srcIp = builder.srcIp;
        this.srcUsername = builder.srcUsername;
        this.srcPassword = builder.srcPassword;
        this.srcFileDir = builder.srcFileDir;
        this.srcFilePrefix = builder.srcFilePrefix;
        this.srcFileDate = builder.srcFileDate;
        this.srcFileType = builder.srcFileType;
        this.destFileDir = builder.destFileDir;
        this.unzipPath = builder.unzipPath;
    }

    public static final class Builder {
        private final String srcIp;
        private final String srcUsername;
        private final String srcPassword;
        private final String srcFileDir;

        private String srcFilePrefix;
        private String srcFileDate;
        private String srcFileType;
        private String destFileDir;
        private String unzipPath;

        public Builder(String srcIp, String srcUsername, String srcPassword, String srcFileDir) {
            this.srcIp = srcIp;
            this.srcUsername = srcUsername;
            this.srcPassword = srcPassword;
            this.srcFileDir = srcFileDir;
        }


        public Builder srcFilePrefix(String srcFilePrefix) {
            this.srcFilePrefix = srcFilePrefix;
            return this;
        }

        public Builder srcFileDate(String srcFileDate) {
            this.srcFileDate = srcFileDate;
            return this;
        }

        public Builder srcFileType(String srcFileType) {
            this.srcFileType = srcFileType;
            return this;
        }

        public Builder destFileDir(String destFileDir) {
            this.destFileDir = destFileDir;
            return this;
        }

        public Builder unzipPath(String unzipPath) {
            this.unzipPath = unzipPath;
            return this;
        }

        public FileProperties build() {
            return new FileProperties(this);
        }
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getSrcUsername() {
        return srcUsername;
    }

    public String getSrcPassword() {
        return srcPassword;
    }

    public String getSrcFileDir() {
        return srcFileDir;
    }

    public String getSrcFilePrefix() {
        return srcFilePrefix;
    }

    public String getSrcFileDate() {
        return srcFileDate;
    }

    public String getSrcFileType() {
        return srcFileType;
    }

    public String getDestFileDir() {
        return destFileDir;
    }

    public String getUnzipPath() {
        return unzipPath;
    }
}
