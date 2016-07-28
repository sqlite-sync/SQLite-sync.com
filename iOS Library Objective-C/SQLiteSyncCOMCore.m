//
//  SQLiteSyncCOMCore.m
//  iOS Library
//
//  Created by Tomasz Dziemidowicz on 30/12/14.
//
//

#import "SQLiteSyncCOMCore.h"
#import "SQLiteSyncCOMSync.h"
#import "XMLReader.h"

@implementation SQLiteSyncCOMCore

SQLiteSyncCOMSync *syncService;

-(id)initWithDatabaseFilename: (NSString*)dbFilename serviceUrl:(NSString*)serviceUrl
{
    self = [super init];
    sqliteDbManager = [[DBManager alloc] initWithDatabaseFilename:dbFilename];
    syncService = [[SQLiteSyncCOMSync alloc] initWithServiceUrl:serviceUrl];
    return self;
}

- (void)reinitializeDB:(NSString *)subscriberId{
    [self reinitializeDBCommit: [syncService GetFullDBSchema:subscriberId]];
}

- (void)sendAndRecieveChanges:(NSString *)subscriberId{
    [self sendChanges:subscriberId];
    [self recieveChanges:subscriberId];
}

- (void)sendChanges:(NSString *)subscriberId{
    NSArray *recordsToSync;
    NSArray *colNames;
    NSString *colValue;
    NSString *colName;
    
    NSArray *arrTablesToSync = [[NSArray alloc] initWithArray:[sqliteDbManager loadDataFromDB:@"select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';"]];
    NSMutableString *sqlitesync_SyncDataToSend = [NSMutableString string];
    
    [sqlitesync_SyncDataToSend appendString:@"<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">"];
    
    for(NSArray *table in arrTablesToSync){
        NSString *tableName = table[0];
        
        if([tableName caseInsensitiveCompare:@"MergeDelete"] != NSOrderedSame
           && [tableName caseInsensitiveCompare:@"MergeIdentity"] != NSOrderedSame) {
            [sqlitesync_SyncDataToSend appendFormat:@"<tab n=\"%@\">",tableName];
            
            [sqlitesync_SyncDataToSend appendString:@"<ins>"];
            recordsToSync = [sqliteDbManager loadDataFromDB:[NSString stringWithFormat:@"select * from %@ where RowId is null;", tableName]];
            colNames = [sqliteDbManager arrColumnNames];
            for(NSArray *record in recordsToSync){
                [sqlitesync_SyncDataToSend appendString:@"<r>"];
                for(int i = 0; i < record.count; i++){
                    colValue = record[i];
                    colName = colNames[i];
                    if([colName caseInsensitiveCompare:@"MergeUpdate"] != NSOrderedSame){
                        [sqlitesync_SyncDataToSend appendFormat:@"<%@>", colName];
                        [sqlitesync_SyncDataToSend appendFormat:@"<![CDATA[%@]]>", colValue];
                        [sqlitesync_SyncDataToSend appendFormat:@"</%@>", colName];
                    }
                }
                [sqlitesync_SyncDataToSend appendString:@"</r>"];
            }
            [sqlitesync_SyncDataToSend appendString:@"</ins>"];
            
            [sqlitesync_SyncDataToSend appendString:@"<upd>"];
            recordsToSync = [sqliteDbManager loadDataFromDB:[NSString stringWithFormat:@"select * from %@ where MergeUpdate > 0 and RowId is not null;", tableName]];
            colNames = [sqliteDbManager arrColumnNames];
            for(NSArray *record in recordsToSync){
                [sqlitesync_SyncDataToSend appendString:@"<r>"];
                for(int i = 0; i < record.count; i++){
                    colValue = record[i];
                    colName = colNames[i];
                    if([colName caseInsensitiveCompare:@"MergeUpdate"] != NSOrderedSame){
                        [sqlitesync_SyncDataToSend appendFormat:@"<%@>", colName];
                        [sqlitesync_SyncDataToSend appendFormat:@"<![CDATA[%@]]>", colValue];
                        [sqlitesync_SyncDataToSend appendFormat:@"</%@>", colName];
                    }
                }
                [sqlitesync_SyncDataToSend appendString:@"</r>"];
            }
            [sqlitesync_SyncDataToSend appendString:@"</upd>"];
            
            [sqlitesync_SyncDataToSend appendString:@"</tab>"];
        }
    }
    
    NSString *TableId;
    NSString *RowId;
    [sqlitesync_SyncDataToSend appendString:@"<delete>"];
    recordsToSync = [sqliteDbManager loadDataFromDB:@"select * from MergeDelete;"];
    colNames = [sqliteDbManager arrColumnNames];
    for(NSArray *record in recordsToSync){
        TableId = RowId = nil;
        for(int i = 0; i < colNames.count; i++){
            if([colNames[i] caseInsensitiveCompare:@"TableId"] == NSOrderedSame){
                TableId = record[i];
            }
            if([colNames[i] caseInsensitiveCompare:@"RowId"] == NSOrderedSame){
                RowId = record[i];
            }
        }
        if(TableId != nil && RowId != nil){
            [sqlitesync_SyncDataToSend appendString:@"<r>"];
            [sqlitesync_SyncDataToSend appendFormat:@"<tb>%@</tb>", TableId];
            [sqlitesync_SyncDataToSend appendFormat:@"<id>%@</id>", RowId];
            [sqlitesync_SyncDataToSend appendString:@"</r>"];
        }
    }
    [sqlitesync_SyncDataToSend appendString:@"</delete>"];
    [sqlitesync_SyncDataToSend appendString:@"</SyncData>"];
    
    [syncService ReceiveData:subscriberId data:sqlitesync_SyncDataToSend];
    
    for(NSArray *table in arrTablesToSync){
        NSString *tableName = table[0];
        if([tableName caseInsensitiveCompare:@"MergeDelete"] != NSOrderedSame
           && [tableName caseInsensitiveCompare:@"MergeIdentity"] != NSOrderedSame) {
            NSString *updTriggerSQL = [sqliteDbManager loadDataFromDB:[NSString stringWithFormat:@"select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_%@'", tableName]][0][0];
            [sqliteDbManager executeNonQuery:[NSString stringWithFormat:@"drop trigger trMergeUpdate_%@;", tableName]];
            [sqliteDbManager executeNonQuery:[NSString stringWithFormat:@"update %@ set MergeUpdate=0 where MergeUpdate > 0;", tableName]];
            [sqliteDbManager executeNonQuery:updTriggerSQL];
        }
    }
    [sqliteDbManager executeNonQuery:@"delete from MergeDelete"];
}

- (void)recieveChanges:(NSString *)subscriberId{
    NSArray *arrTablesToSync = [[NSArray alloc] initWithArray:[sqliteDbManager loadDataFromDB:@"select tbl_Name from sqlite_master where type='table'"]];
    for (int i=0; i<arrTablesToSync.count; i++) {
        NSString *tableName = arrTablesToSync[i][0];
        [self commitChangesForTable: [syncService GetDataForSync:subscriberId tableName:tableName]];
    }
}
- (void) reinitializeDBCommit:(NSString *)reinitializeScript{
    NSDictionary *sqliteReinitializeArray =
    [NSJSONSerialization JSONObjectWithData: [reinitializeScript dataUsingEncoding:NSUTF8StringEncoding]
                                    options: NSJSONReadingMutableContainers
                                      error: nil];
    
    NSArray *keys = [sqliteReinitializeArray allKeys];
    NSArray *sortedsqliteReinitializeArray = [keys sortedArrayUsingComparator:^NSComparisonResult(id a, id b) {
        return [a compare:b];
    }];
    
    BOOL *versionKeyFounded = false;
    
    for (NSString* key in sortedsqliteReinitializeArray) {
        if([key containsString:@"00000"]){
            versionKeyFounded = true;
            id value = [sqliteReinitializeArray objectForKey:key];
            if([value intValue] < 21){
                NSLog(@"Wrong SQLiteSync server version.");
                return;
            }
        }
    }
    if(!versionKeyFounded){
        NSLog(@"Wrong SQLiteSync server version.");
        return;
    }
    
    for (NSString* key in sortedsqliteReinitializeArray) {
        if(![key containsString:@"00000"]){
            id value = [sqliteReinitializeArray objectForKey:key];
            [sqliteDbManager executeNonQuery:value];
        }
    }    
}

-(void)commitChangesForTable:(NSString *)response{
    NSDictionary *recordsDct;
    NSDictionary *columnsDct;
    NSArray *records;
    NSArray *columns;
    NSString *sAction;
    int action;
    long SyncId;

    NSArray<SQLiteSyncDataObject> *dataObjects = [SQLiteSyncDataObject arrayOfModelsFromString:response];
    for(SQLiteSyncDataObject *dataObject in dataObjects){
        records = [NSArray new];
        SyncId = [dataObject.SyncId integerValue];
        
        if (SyncId > 0){
            @try {
                [sqliteDbManager executeNonQuery:[dataObject TriggerInsertDrop]];
                [sqliteDbManager executeNonQuery:[dataObject TriggerUpdateDrop]];
                [sqliteDbManager executeNonQuery:[dataObject TriggerDeleteDrop]];
                
                recordsDct=[[[NSDictionary alloc]initWithDictionary:[XMLReader dictionaryForXMLString:[dataObject Records] error:nil]] valueForKeyPath:@"records.r"];
                
                if([recordsDct isKindOfClass:[NSArray class]]){
                    records = (NSArray*)recordsDct;
                }else if([recordsDct isKindOfClass:[NSDictionary class]]){
                    records = [NSArray arrayWithObject:recordsDct];
                }
                
                for(NSDictionary *record in records){
                    sAction = [record valueForKey:@"a"];
                    action = [sAction intValue];
                    
                    columnsDct=[record valueForKeyPath:@"c.innerValue"];
                    columns = [NSArray new];
                    if([columnsDct isKindOfClass:[NSArray class]]){
                        columns = (NSArray*)columnsDct;
                    }else {
                        columns = [NSArray arrayWithObject:columnsDct];
                    }
                    
                    switch (action) {
                        case 1:
                            [sqliteDbManager executeNonQuery:[dataObject QueryInsert] parameters:columns];
                            break;
                        case 2:
                            [sqliteDbManager executeNonQuery:[dataObject QueryUpdate] parameters:columns];
                            break;
                        case 3:
                            [sqliteDbManager executeNonQuery:[[dataObject QueryDelete] stringByAppendingString:@"?"] parameters:columns];
                            break;
                        default:
                            break;
                    }
                }
                
                [sqliteDbManager executeNonQuery:[dataObject TriggerInsert]];
                [sqliteDbManager executeNonQuery:[dataObject TriggerUpdate]];
                [sqliteDbManager executeNonQuery:[dataObject TriggerDelete]];
            
                [syncService SyncCompleted:SyncId];
                
            } @catch (NSException *exception) {
                NSLog(@"commitChangesForTable Exception: %@",exception);
            }
        }
    }
}

@end