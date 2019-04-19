package SQLiteSyncCore.SyncServer.Synchronization;

import SQLiteSyncCore.Logs;
import jersey.repackaged.com.google.common.cache.CacheBuilder;
import jersey.repackaged.com.google.common.cache.CacheLoader;
import jersey.repackaged.com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//import jersey.repackaged.com.google.common.CacheLoader;
//import jersey.repackaged.com.google.common.LoadingCache;

/*************************************************************************
 *
 * CONFIDENTIAL
 * __________________
 *
 *  AMPLIFIER sp. z o.o.
 *  www.ampliapps.com
 *  support@ampliapps.com
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AMPLIFIER sp. z o.o. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to AMPLIFIER sp. z o.o.
 * and its suppliers and may be covered by U.S., European and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AMPLIFIER sp. z o.o..
 **************************************************************************/

public class DatabaseTableGuavaCacheUtil {
    private static LoadingCache<String, DatabaseTable> databaseTablesCache;
    static {
        databaseTablesCache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, DatabaseTable>() {
                            @Override
                            public DatabaseTable load(String key) throws Exception {
                                //key == schema.table_name
                                String tableName = key;
                                String schema = "";
                                if(key.contains("__s__")) {
                                    String[] tmp = key.split("__s__");
                                    schema = tmp[0];
                                    tableName = tmp[1];
                                }
                                return new DatabaseTable(tableName, schema);
                            }
                        }
                );
    }
    private static LoadingCache<String, DatabaseTable> getLoadingCache() {
        return databaseTablesCache;
    }

    public static DatabaseTable getTableUsingGuava(String tableName, String schema) {
        try {
            LoadingCache<String, DatabaseTable> tablesCache = DatabaseTableGuavaCacheUtil.getLoadingCache();
            String key = tableName;
            if (schema != null && !schema.isEmpty())
                key = schema + "__s__" + tableName;
            return tablesCache.get(key);
        } catch (ExecutionException e){
            Logs.write(Logs.Level.ERROR, "getTableUsingGuava() " + e.getMessage());
            return new DatabaseTable(tableName, schema);
        }
    }
}
