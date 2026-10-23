package net.kdt.pojavlaunch.utils;

import java.util.UUID;
import net.kdt.pojavlaunch.authenticator.UserIdentity;

public final class MCOptionUtils {
    
    public static UserIdentity createLogin(String username) {
        UserIdentity identity = new UserIdentity();
        identity.username = username;
        // Генеруємо валідний UUID замість нулів, щоб гра не скидала ліцензію в Demo
        identity.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
        identity.accessToken = "minlauncher_vulkan_token";
        // Примусово виставляем статус PREMIUM розробника, прибираючи Demo-режим
        identity.userType = "mojang"; 
        identity.canPlaySelectedVersion = true;
        return identity;
    }
}
