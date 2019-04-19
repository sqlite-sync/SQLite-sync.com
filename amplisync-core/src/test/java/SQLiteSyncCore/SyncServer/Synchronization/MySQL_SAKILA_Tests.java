package SQLiteSyncCore.SyncServer.Synchronization;

import SQLiteSyncCore.Logs;
import SQLiteSyncCore.SQLiteSyncConfig;
import SQLiteSyncCore.SyncServer.CommonTools;
import SQLiteSyncCore.SyncServer.SchemaPublish.SchemaGenerator;
import org.apache.logging.log4j.LogManager;

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

public class MySQL_SAKILA_Tests {
    MySQL_SAKILA_Tests(){

    }

    @org.junit.jupiter.api.Test
    public void testReinitialization(){
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        String json = schemaGenerator.GetFullSchematScript("1");
        Logs.write(Logs.Level.INFO, json);
    }

    @org.junit.jupiter.api.Test
    public void AddAllTablesToSynchronization(){
        CommonTools commonTools = new CommonTools();

        commonTools.AddTableToSynchronization("actor");
        commonTools.AddTableToSynchronization("address");
        commonTools.AddTableToSynchronization("category");
        commonTools.AddTableToSynchronization("city");
        commonTools.AddTableToSynchronization("country");
        commonTools.AddTableToSynchronization("customer");
        commonTools.AddTableToSynchronization("film");
        commonTools.AddTableToSynchronization("film_actor");
        commonTools.AddTableToSynchronization("film_category");
        commonTools.AddTableToSynchronization("film_text");
        commonTools.AddTableToSynchronization("inventory");
        commonTools.AddTableToSynchronization("language");
        commonTools.AddTableToSynchronization("payment");
        commonTools.AddTableToSynchronization("rental");
        commonTools.AddTableToSynchronization("staff");
        commonTools.AddTableToSynchronization("store");
    }

    @org.junit.jupiter.api.Test
    public void RemoveAllTablesToSynchronization(){
        CommonTools commonTools = new CommonTools();
        commonTools.RemoveTableFromSynchronization("actor");
        commonTools.RemoveTableFromSynchronization("address");
        commonTools.RemoveTableFromSynchronization("category");
        commonTools.RemoveTableFromSynchronization("city");
        commonTools.RemoveTableFromSynchronization("country");
        commonTools.RemoveTableFromSynchronization("customer");
        commonTools.RemoveTableFromSynchronization("film");
        commonTools.RemoveTableFromSynchronization("film_actor");
        commonTools.RemoveTableFromSynchronization("film_category");
        commonTools.RemoveTableFromSynchronization("film_text");
        commonTools.RemoveTableFromSynchronization("inventory");
        commonTools.RemoveTableFromSynchronization("language");
        commonTools.RemoveTableFromSynchronization("payment");
        commonTools.RemoveTableFromSynchronization("rental");
        commonTools.RemoveTableFromSynchronization("staff");
        commonTools.RemoveTableFromSynchronization("store");
    }

    @org.junit.jupiter.api.Test
    public void SyncGetData(){
        SyncService syncService = new SyncService();

        syncService.DoSync("1","address");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","category");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","city");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","country");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","customer");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","film");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","film_actor");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","film_category");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","film_text");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","inventory");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","language");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","payment");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","rental");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","staff");
        SyncCommitSync(syncService.syncIdForTestPurpose);
        syncService.DoSync("1","store");
        SyncCommitSync(syncService.syncIdForTestPurpose);
    }

    @org.junit.jupiter.api.Test
    void SyncCommitSync(Integer syncId){
        SyncService syncService = new SyncService();
        syncService.CommitSync(syncId.toString());
    }

    @org.junit.jupiter.api.Test
    void SyncCommitSync(){
        SyncService syncService = new SyncService();
        syncService.CommitSync("5");
    }

    @org.junit.jupiter.api.Test
    void RecieveData(){
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\"><tab n=\"MergeIdentity\"><ins></ins><upd></upd></tab><tab n=\"actor\"><ins></ins><upd></upd></tab><tab n=\"address\"><ins></ins><upd></upd></tab><tab n=\"category\"><ins></ins><upd></upd></tab><tab n=\"city\"><ins></ins><upd></upd></tab><tab n=\"country\"><ins></ins><upd></upd></tab><tab n=\"customer\"><ins></ins><upd></upd></tab><tab n=\"film\"><ins></ins><upd></upd></tab><tab n=\"film_actor\"><ins></ins><upd></upd></tab><tab n=\"film_category\"><ins></ins><upd></upd></tab><tab n=\"film_text\"><ins></ins><upd></upd></tab><delete></delete></SyncData>";
        DeviceDataObject obj = new DeviceDataObject();
        obj.setContent(xml);
        obj.setSubscriber("1");
        obj.setVersion("3");

        SyncService syncService = new SyncService();
        syncService.ReceiveData(obj);
    }

    @org.junit.jupiter.api.Test
    void SynchronizeStress(){
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
        RecieveData();
        SyncGetData();
    }
}
