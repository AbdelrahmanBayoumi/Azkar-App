package com.bayoumi.util.db;

import com.bayoumi.util.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Generic key–value store backed by a table of (key TEXT PRIMARY KEY, value TEXT).
 *
 * @param <K> any Enum whose name() is used as the key.
 */
public abstract class KeyValueStore<K extends Enum<K> & KeyValueDefault> {
    private final String tableName;
    private final Class<K> keyClass;

    protected KeyValueStore(String tableName, Class<K> keyClass) {
        this.tableName = tableName;
        this.keyClass = keyClass;
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        try {
            DatabaseManager.getInstance()
                    .con
                    .prepareStatement("CREATE TABLE IF NOT EXISTS " + tableName + " ( key TEXT PRIMARY KEY, value TEXT );")
                    .execute();
            // TODO: add indexes
        } catch (Exception ex) {
            throw new RuntimeException("Failed to init table " + tableName, ex);
        }
    }

    public void set(K key, String value) {
        if (exists(key)) {
            Logger.debug("[KeyValueStore] Update >> " + key + ": [" + value + "]");
            update(key, value);
        } else {
            Logger.debug("[KeyValueStore] Insert >> " + key + ": [" + value + "]");
            insert(key, value);
        }
    }

    private boolean exists(K key) {
        try {
            final String sql = "SELECT 1 FROM " + tableName + " WHERE key=?";
            final PreparedStatement ps = DatabaseManager.getInstance().con.prepareStatement(sql);
            ps.setString(1, key.getName());
            return ps.executeQuery().next();
        } catch (Exception ex) {
            Logger.error("Exists check failed on " + tableName, ex, getClass().getName() + ".exists()");
        }
        return false;
    }

    private void insert(K key, String value) {
        try {
            final String sql = "INSERT INTO " + tableName + " (key,value) VALUES(?,?)";
            final PreparedStatement ps = DatabaseManager.getInstance().con.prepareStatement(sql);
            ps.setString(1, key.getName());
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception ex) {
            Logger.error("Insert failed on " + tableName, ex, getClass().getName() + ".insert()");
        }
    }

    private void update(K key, String value) {
        try {
            final String sql = "UPDATE " + tableName + " SET value=? WHERE key=?";
            final PreparedStatement ps = DatabaseManager.getInstance().con.prepareStatement(sql);
            ps.setString(1, value);
            ps.setString(2, key.getName());
            ps.executeUpdate();
        } catch (Exception ex) {
            Logger.error("Update failed on " + tableName, ex, getClass().getName() + ".update()");
        }
    }

    protected abstract List<K> getKeysThatDoNotAllowedToHaveEmptyValues();

    public String get(K key, String defaultValue) {
        try {
            final String sql = "SELECT value FROM " + tableName + " WHERE key=?";
            final PreparedStatement ps = DatabaseManager.getInstance().con.prepareStatement(sql);
            ps.setString(1, key.getName());
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final String value = rs.getString("value");

                // If the retrieved value is empty, and the key is not allowed to have an empty value,
                // update the stored value to the default since empty strings are not permitted.
                if (value != null && value.isEmpty() && !getKeysThatDoNotAllowedToHaveEmptyValues().contains(key)) {
                    update(key, defaultValue);
                    return defaultValue;
                }

                // Return the existing value or default if value is null
                return value != null ? value : defaultValue;
            }

            // not found → insert default
            set(key, defaultValue);
        } catch (Exception ex) {
            Logger.error("Read error from " + tableName, ex, getClass().getName() + ".get()");
        }

        return defaultValue;
    }

    public Map<K, String> getAllValues() {
        final Map<K, String> map = new EnumMap<>(keyClass);
        try {
            final String sql = "SELECT key,value FROM " + tableName;
            final ResultSet rs = DatabaseManager.getInstance().con.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                final String keyName = rs.getString("key");
                for (K k : keyClass.getEnumConstants()) {
                    if (k.getName().equals(keyName)) {
                        map.put(k, rs.getString("value"));
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.error("getAllValues failed on " + tableName, ex, getClass().getName() + ".getAllValues()");
        }
        return map;
    }

    public String get(K key) {
        return get(key, key.getDefaultValue());
    }

    public boolean getBoolean(K key) {
        return get(key).equalsIgnoreCase("true");
    }

    public int getInt(K key) {
        return Integer.parseInt(get(key));
    }

    public double getDouble(K key) {
        return Double.parseDouble(get(key));
    }
}
