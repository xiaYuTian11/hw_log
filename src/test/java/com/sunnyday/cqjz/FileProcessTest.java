package com.sunnyday.cqjz;

import cn.hutool.core.io.resource.ClassPathResource;
import com.sunnyday.cqjz.entity.SSHLoginInfo;
import com.sunnyday.cqjz.utils.SSHRemoteUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * @author TMW
 * @since 2020/11/18 10:52
 */
public class FileProcessTest {

    @Test
    public void test() throws IOException {
        ClassPathResource resource = new ClassPathResource("file.properties");
        Properties properties = new Properties();
        properties.load(resource.getStream());
        final String ip = properties.getProperty("src.ssh.ip");
        final String username = properties.getProperty("src.ssh.username");
        final String password = properties.getProperty("src.ssh.password");

        SSHRemoteUtil remoteUtil = SSHRemoteUtil.getInstance();
        remoteUtil.sshRemoteCallLogin(new SSHLoginInfo.Builder()
                .setIpAddress(ip).setUserName(username).setPassWord(password).build());
        //  exec_command 每次执行后，不会保存会话。把要执行的多条命令放在一个字符串中，使用；分隔。
        remoteUtil.execCommand("cd /tmp; pwd ; ls;");
        remoteUtil.closeSession();
    }

}