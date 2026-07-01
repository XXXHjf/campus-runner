package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.AdminStatusConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.exception.AdminException;
import com.mikasa.campusrunner.common.utils.EncryptSHA256Util;
import com.mikasa.campusrunner.mapper.AdminMapper;
import com.mikasa.campusrunner.pojo.dto.admin.AdminLoginDTO;
import com.mikasa.campusrunner.pojo.dto.admin.AdminRegisterDTO;
import com.mikasa.campusrunner.pojo.entity.Admin;
import com.mikasa.campusrunner.service.admin.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Enumeration;

/**
 * author  Edith
 * created  2025/12/14 11:08
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 管理员用户登录
     * @param adminLoginDTO
     * @return
     */
    @Override
    @Transactional
    public Admin login(AdminLoginDTO adminLoginDTO) {
        String passwordHash = EncryptSHA256Util.encrypt(adminLoginDTO.getPassword());
        String username = adminLoginDTO.getUsername();
        //获取用户信息
        Admin admin = adminMapper.getUserByLogin(username, passwordHash);

        if (admin == null) {
            //管理员用户不存在
            throw new AdminException(MessageConstant.NO_ADMIN_USER);
        }

        //更新用户信息
        LocalDateTime now = LocalDateTime.now();
//        String ipAddress = String.valueOf(getLocalHostLANAddress()).substring(1);
//        String macAddress = getMacAddress(ipAddress);

        String loginIP = getIPv4AndMacAddress();

        admin.setLastLoginTime(now);
        admin.setLastLoginIp(loginIP);
        admin.setUpdateTime(now);

        adminMapper.update(admin);

        return admin;
    }

    /**
     * 挂你来源用户注册
     * @param adminRegisterDTO
     */
    @Override
    @Transactional
    public void register(AdminRegisterDTO adminRegisterDTO) {
        log.info("Admin user registration in progress...");
        Admin admin = new Admin();
        BeanUtils.copyProperties(adminRegisterDTO, admin);
        String loginIP = getIPv4AndMacAddress();//获取IPv4地址和Mac地址
        LocalDateTime now = LocalDateTime.now();
        String passwordHash = EncryptSHA256Util.encrypt(adminRegisterDTO.getPassword());//获得密码的哈希值


        admin.setStatus(AdminStatusConstant.IS_SUPER_ADMINISTRATOR); //设置管理员类别状态
        admin.setSchool(Long.valueOf(AdminStatusConstant.IS_SUPER_ADMINISTRATOR)); //设置school字段
        admin.setLastLoginIp(loginIP);
        admin.setLastLoginTime(now);
        admin.setCreateTime(now);
        admin.setUpdateTime(now);
        admin.setPasswordHash(passwordHash);

        adminMapper.insert(admin);
    }

    /**
     * 辅助方法
     * 获取IPv4地址和MAC物理硬件地址
     * 形如 IPv4: 192.168.22.12, MAC: ff-ee-dd-cc-bb-aa
     * @return
     */
    private String getIPv4AndMacAddress() {
        String ipAddress = String.valueOf(getLocalHostLANAddress()).substring(1);
        String macAddress = getMacAddress(ipAddress);
        String loginIP = "IPv4: " + ipAddress + ", MAC: " + macAddress;
        return loginIP;
    }

    /**
     * 辅助方法
     * 获取本机的ipv4地址
     * @return
     * @throws UnknownHostException
     */
    private InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            return null;
//            throw unknownHostException;
        }
    }


    /**
     * 辅助方法
     * 获取当前ip下的MAC地址，即物理硬件地址
     * @param ip
     * @return
     */
    private String getMacAddress(String ip) {
        try {
            InetAddress localIP = InetAddress.getByName(ip);
            NetworkInterface ni = NetworkInterface.getByInetAddress(localIP);
            byte[] hardwareAddress = ni.getHardwareAddress();
            StringBuilder str = new StringBuilder();
            boolean flag = false;
            for (byte b : hardwareAddress) {
                String s = Integer.toHexString(b & 0xFF);
                if (!flag) {
                    str.append(s);
                    flag = true;
                }else {
                    str.append("-");
                    str.append(s);
                }
            }
            return str.toString();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


}
