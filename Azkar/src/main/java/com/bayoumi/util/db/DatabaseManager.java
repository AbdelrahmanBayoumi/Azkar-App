package com.bayoumi.util.db;

import java.time.LocalDateTime;

public class DatabaseManager {
    public static LocalDateTime getStoredDate() {
        return LocalDateTime.now();
    }


}
