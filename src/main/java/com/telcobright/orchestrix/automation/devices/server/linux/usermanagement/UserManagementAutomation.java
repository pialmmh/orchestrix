package com.telcobright.orchestrix.automation.devices.server.linux.usermanagement;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.List;

public interface UserManagementAutomation extends LinuxAutomation {
    boolean createUser(SshDevice device, String username, String password) throws Exception;
    boolean deleteUser(SshDevice device, String username) throws Exception;
    boolean addUserToGroup(SshDevice device, String username, String group) throws Exception;
    boolean removeUserFromGroup(SshDevice device, String username, String group) throws Exception;
    boolean changePassword(SshDevice device, String username, String newPassword) throws Exception;
    boolean lockUser(SshDevice device, String username) throws Exception;
    boolean unlockUser(SshDevice device, String username) throws Exception;
    List<String> listUsers(SshDevice device) throws Exception;
    boolean userExists(SshDevice device, String username) throws Exception;
}