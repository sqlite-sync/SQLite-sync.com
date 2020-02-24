//
//  SQLiteSync.h
//
//  Created by sqlite-sync.com
//

#import "SQLiteSync.h"

@interface SQLiteSync()
@property (nonatomic, strong) NSString *databasePath;
@property (nonatomic, strong) NSString *serverURL;
@end

@implementation SQLiteSync
-(nonnull instancetype)initWithDbFileName:(nonnull NSString*)dbFileName serverURL:(nonnull NSString*)serverURL{
    self = [super init];
    if (self) {
        self.serverURL = serverURL;
        
        NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *docsDir = [dirPaths objectAtIndex:0];
        
        self.databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: dbFileName]];
        
        if (![[NSFileManager defaultManager] fileExistsAtPath: self.databasePath ]) {
            sqlite3 *db;
            
            if (sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK) {
                sqlite3_close(db);
            }
            else {
                [NSException raise:@"SQLiteSync_Exception" format:@"Failed to open/create database"];
            }
        }
    }
    return self;
}

-(void)initializeSubscriber:(nonnull NSString*)subscriberId onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish{
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0ul);
    dispatch_async(queue, ^{
        NSError *error;
        [self initializeSubscriber:subscriberId error:&error];
        dispatch_async(dispatch_get_main_queue(), ^{
            onFinish(error);
        });
    });
}

-(void)synchronizeSubscriber:(nonnull NSString*)subscriberId onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish{
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0ul);
    dispatch_async(queue, ^{
        NSError *error;
        [self synchronizeSubscriber:subscriberId error:&error];
        dispatch_async(dispatch_get_main_queue(), ^{
            onFinish(error);
        });
    });
}

-(void)addSynchrnizedTable:(nonnull NSString*)tableName onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish{
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0ul);
    dispatch_async(queue, ^{
        NSError *error;
        [self addSynchrnizedTable:tableName error:&error];
        dispatch_async(dispatch_get_main_queue(), ^{
            onFinish(error);
        });
    });
}

-(void)removeSynchrnizedTable:(nonnull NSString*)tableName onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish{
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0ul);
    dispatch_async(queue, ^{
        NSError *error;
        [self removeSynchrnizedTable:tableName error:&error];
        dispatch_async(dispatch_get_main_queue(), ^{
            onFinish(error);
        });
    });
}

-(void)initializeSubscriber:(nonnull NSString*)subscriberId error:(NSError * _Nullable * _Nullable)error{
    NSString *requestUrlString = [NSString stringWithFormat:@"%@/InitializeSubscriber/%@", _serverURL, subscriberId];
    NSURL *requestURL = [NSURL URLWithString:requestUrlString];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setURL:requestURL];
    [request setHTTPMethod:@"GET"];
    
    NSHTTPURLResponse *response;
    
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];
    
    if(!*error){
        switch (response.statusCode) {
            case 200:
                break;
            default:
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                break;
        }
    }
    
    if(*error) return;
    
    NSDictionary* schema = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:error];
    
    if(*error) return;
    
    NSArray *keys = [[schema allKeys] sortedArrayUsingComparator:^NSComparisonResult(id a, id b) { return [a compare:b]; }];
    
    sqlite3 *db;
    sqlite3_stmt *stmt = NULL;
    
    if(sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK){
        sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
        
        for (NSString* key in keys) {
            if(![key containsString:@"00000"]){
                if(sqlite3_prepare_v2(db, [[schema objectForKey:key] UTF8String], -1, &stmt, NULL) != SQLITE_OK
                   || sqlite3_step(stmt) != SQLITE_DONE){
                    *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                    break;
                }
                sqlite3_reset(stmt);
            }
        }
        
        if(*error){
            sqlite3_exec(db, "ROLLBACK TRANSACTION", 0, 0, 0);
        }
        else{
            sqlite3_exec(db, "COMMIT", 0, 0, 0);
        }
        
        if(stmt){
            sqlite3_finalize(stmt);
        }
        
        sqlite3_close(db);
    }
    else{
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:@"Failed to open database" forKey:NSLocalizedDescriptionKey]];
    }
}

-(void)synchronizeSubscriber:(nonnull NSString*)subscriberId error:(NSError * _Nullable * _Nullable)error{
    [self sendLocalChanges:subscriberId error:error];
    
    if(*error) return;
    
    [self clearChangesMarker:error];
    
    if(*error) return;
    
    NSMutableArray<NSString*> *tables = [NSMutableArray array];
    
    sqlite3 *db;
    sqlite3_stmt *stmt;
    
    if(sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK){
        NSString *query = @"select tbl_Name from sqlite_master where type='table'";
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            while(sqlite3_step(stmt) == SQLITE_ROW) {
                [tables addObject:[NSString stringWithUTF8String:(char *)sqlite3_column_text(stmt, 0)]];
            }
        }
        else{
            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
        }
        sqlite3_finalize(stmt);
        sqlite3_close(db);
    }
    else{
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:@"Failed to open database" forKey:NSLocalizedDescriptionKey]];
    }
    
    if(*error) return;
    
    for(NSString *tableName in tables){
        bool moreData = true;
        while (moreData){
            NSArray<SQLiteSyncData*> *syncDatas = [self getRemoteChangesForTable:subscriberId tableName:tableName error:error];
            if(*error) return;
            moreData = false;
            for(SQLiteSyncData *syncData in syncDatas){
                if([syncData.SyncId intValue] > 0){
                    moreData = moreData || [syncData.MaxPackageSize intValue] == [syncData.RowsCount intValue];
                    [self applyRemoteChangesForTable:syncData error:error];
                }
            }
            if(*error) return;
        }
    }
}

-(void)addSynchrnizedTable:(nonnull NSString*)tableName error:(NSError * _Nullable * _Nullable)error{
    NSString *requestUrlString = [NSString stringWithFormat:@"%@/AddTable/%@", _serverURL, tableName];
    NSURL *requestURL = [NSURL URLWithString:requestUrlString];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setURL:requestURL];
    [request setHTTPMethod:@"GET"];
    
    NSHTTPURLResponse *response;
    
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];
    
    if(!*error){
        switch (response.statusCode) {
            case 200:
                break;
            default:
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                break;
        }
    }
}

-(void)removeSynchrnizedTable:(nonnull NSString*)tableName error:(NSError * _Nullable * _Nullable)error{
    NSString *requestUrlString = [NSString stringWithFormat:@"%@/RemoveTable/%@", _serverURL, tableName];
    NSURL *requestURL = [NSURL URLWithString:requestUrlString];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setURL:requestURL];
    [request setHTTPMethod:@"GET"];
    
    NSHTTPURLResponse *response;
    
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];
    
    if(!*error){
        switch (response.statusCode) {
            case 200:
                break;
            default:
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                break;
        }
    }
}

-(void)sendLocalChanges:(NSString*)subscriberId error:(NSError **)error{
    int limit = 1000;
    int offset = 0;
    
    while (true) {
        NSString *changes = [self getLocalChanges:subscriberId limit:limit offset:offset error:error];
        offset += limit;
        
        if(*error) return;
        
        if(!changes) return;
        
        NSDictionary *inputObject = @{@"subscriber": subscriberId, @"content": changes, @"version": @"3"};
        NSData *inputData = [NSJSONSerialization dataWithJSONObject:inputObject options:NSJSONWritingPrettyPrinted error:error];
        
        if(*error) return;
        
        NSString *requestUrlString = [NSString stringWithFormat:@"%@/Send", _serverURL];
        NSURL *requestURL = [NSURL URLWithString:requestUrlString];
        NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
        
        [request setURL:requestURL];
        [request setHTTPMethod:@"POST"];
        [request setHTTPBody:inputData];
        [request setValue:@"application/json; charset=utf-8" forHTTPHeaderField:@"Content-Type"];
        [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)inputData.length] forHTTPHeaderField:@"Content-Length"];
        
        NSHTTPURLResponse *response;
        
        NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];
        
        if(!*error){
            switch (response.statusCode) {
                case 200:
                    break;
                case 204:
                    break;
                default:
                    *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                    break;
            }
        }
    }
}

-(NSString*)getLocalChanges:(NSString*)subscriberId limit:(int)limit offset:(int)offset error:(NSError **)error {
    sqlite3 *db;
    sqlite3_stmt *stmt;
    NSString *query;
    bool noChanges = true;
    
    NSMutableString *builder = [NSMutableString string];
    
    [builder appendString:@"<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">"];
    
    if(sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK){
        NSMutableArray<NSString*> *tables = [NSMutableArray array];
        
        query = @"select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';";
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            while(sqlite3_step(stmt) == SQLITE_ROW) {
                [tables addObject:[NSString stringWithUTF8String:(char *)sqlite3_column_text(stmt, 0)]];
            }
        }
        else{
            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
        }
        
        if(!*error){
            for(NSString *tableName in tables){
                if([tableName caseInsensitiveCompare:@"MergeDelete"] != NSOrderedSame){
                    [builder appendFormat:@"<tab n=\"%@\">", tableName];
                    
                    [builder appendString:@"<ins>"];
                    query = [NSString stringWithFormat:@"select * from %@ where RowId is null LIMIT %d OFFSET %d;", tableName, limit, offset];
                    if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                        while(sqlite3_step(stmt) == SQLITE_ROW) {
                            noChanges = false;
                            [builder appendString:@"<r>"];
                            for(int i = 0; i < sqlite3_column_count(stmt); i++){
                                NSString *columnName = [NSString stringWithUTF8String:sqlite3_column_name(stmt, i)];
                                if([columnName caseInsensitiveCompare:@"MergeUpdate"] != NSOrderedSame){
                                    [builder appendFormat:@"<%1$@><![CDATA[%2$s]]></%1$@>", columnName, sqlite3_column_text(stmt, i)];
                                }
                            }
                            [builder appendString:@"</r>"];
                        }
                    }
                    else{
                        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                        break;
                    }
                    [builder appendString:@"</ins>"];
                    
                    [builder appendString:@"<upd>"];
                    query = [NSString stringWithFormat:@"select * from %@ where MergeUpdate > 0 and RowId is not null LIMIT %d OFFSET %d;", tableName, limit, offset];
                    if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                        while(sqlite3_step(stmt) == SQLITE_ROW) {
                            noChanges = false;
                            [builder appendString:@"<r>"];
                            for(int i = 0; i < sqlite3_column_count(stmt); i++){
                                NSString *columnName = [NSString stringWithUTF8String:sqlite3_column_name(stmt, i)];
                                if([columnName caseInsensitiveCompare:@"MergeUpdate"] != NSOrderedSame){
                                    [builder appendFormat:@"<%1$@><![CDATA[%2$s]]></%1$@>", columnName, sqlite3_column_text(stmt, i)];
                                }
                            }
                            [builder appendString:@"</r>"];
                        }
                    }
                    else{
                        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                        break;
                    }
                    [builder appendString:@"</upd>"];
                    
                    [builder appendString:@"</tab>"];
                }
            }
        }
        
        if(!*error){
            [builder appendString:@"<delete>"];
            query = [NSString stringWithFormat:@"select TableId,RowId from MergeDelete LIMIT %d OFFSET %d;", limit, offset];
            if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                while(sqlite3_step(stmt) == SQLITE_ROW) {
                    noChanges = false;
                    [builder appendFormat:@"<r><tb>%1$s</tb><id>%2$s</id></r>", sqlite3_column_text(stmt, 0), sqlite3_column_text(stmt, 1)];
                }
            }
            else{
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
            [builder appendString:@"</delete>"];
        }
        
        if(stmt){
            sqlite3_finalize(stmt);
        }
        sqlite3_close(db);
    }
    else{
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:@"Failed to open database" forKey:NSLocalizedDescriptionKey]];
    }
    
    if(*error) return nil;
    
    [builder appendString:@"</SyncData>"];
    
    if(noChanges) return nil;
    
    return builder;
}

-(void)clearChangesMarker:(NSError **)error{
    sqlite3 *db;
    sqlite3_stmt *stmt;
    NSString *query;
    
    if(sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK){
        sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
        
        NSMutableArray<NSString*> *tables = [NSMutableArray array];
        
        query = @"select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';";
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            while(sqlite3_step(stmt) == SQLITE_ROW) {
                [tables addObject:[NSString stringWithUTF8String:(char *)sqlite3_column_text(stmt, 0)]];
            }
        }
        else{
            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
        }
        sqlite3_reset(stmt);
        
        for(NSString *tableName in tables){
            if([tableName caseInsensitiveCompare:@"MergeIdentity"] == NSOrderedSame) {
                query = @"update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;";
                if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) != SQLITE_OK
                   || sqlite3_step(stmt) != SQLITE_DONE){
                    *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                    break;
                }
                sqlite3_reset(stmt);
            }
            else if([tableName caseInsensitiveCompare:@"MergeDelete"] != NSOrderedSame) {
                NSString *updTriggerSQL;
                
                query = [NSString stringWithFormat:@"select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_%@'", tableName];
                if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK
                   && sqlite3_step(stmt) == SQLITE_ROW){
                    updTriggerSQL = [NSString stringWithUTF8String:(char *)sqlite3_column_text(stmt, 0)];
                }
                sqlite3_reset(stmt);
                
                if(updTriggerSQL){
                    if(!*error){
                        query = [NSString stringWithFormat:@"drop trigger trMergeUpdate_%@;", tableName];
                        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) != SQLITE_OK
                           || sqlite3_step(stmt) != SQLITE_DONE){
                            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                            break;
                        }
                        sqlite3_reset(stmt);
                    }
                    if(!*error){
                        query = [NSString stringWithFormat:@"update %@ set MergeUpdate=0 where MergeUpdate > 0;", tableName];
                        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) != SQLITE_OK
                           || sqlite3_step(stmt) != SQLITE_DONE){
                            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                            break;
                        }
                        sqlite3_reset(stmt);
                    }
                    if(!*error){
                        query = updTriggerSQL;
                        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) != SQLITE_OK
                           || sqlite3_step(stmt) != SQLITE_DONE){
                            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                            break;
                        }
                        sqlite3_reset(stmt);
                    }
                }
            }
        }
        
        if(!*error){
            query = @"delete from MergeDelete";
            if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
        }
        
        if(*error){
            sqlite3_exec(db, "ROLLBACK TRANSACTION", 0, 0, 0);
        }
        else{
            sqlite3_exec(db, "COMMIT", 0, 0, 0);
        }
        
        if(stmt){
            sqlite3_finalize(stmt);
        }
        
        sqlite3_close(db);
    }
    else{
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:@"Failed to open database" forKey:NSLocalizedDescriptionKey]];
    }
}

-(NSArray<SQLiteSyncData*>*)getRemoteChangesForTable:(NSString*)subscriberId tableName:(NSString*)tableName error:(NSError **)error{
    NSString *requestUrlString = [NSString stringWithFormat:@"%@/Sync/%@/%@", _serverURL, subscriberId, tableName];
    NSURL *requestURL = [NSURL URLWithString:requestUrlString];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setURL:requestURL];
    [request setHTTPMethod:@"GET"];
    
    NSHTTPURLResponse *response;
    
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];

    if(!*error){
        switch (response.statusCode) {
            case 200:
                break;
            default:
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                break;
        }
    }
    
    if(*error) return nil;
    
    NSArray<SQLiteSyncData*> *syncDatas = [SQLiteSyncData arrayOfModelsFromData:data error:error];
    
    if(*error) return nil;
    
    return syncDatas;
}

-(void)applyRemoteChangesForTable:(SQLiteSyncData*)syncData error:(NSError **)error {
    
    sqlite3 *db;
    sqlite3_stmt *stmt = nil;
    
    if(sqlite3_open([_databasePath UTF8String], &db) == SQLITE_OK){
        sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
        
        if(!*error && [syncData.TriggerInsertDrop length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerInsertDrop UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
        }
        if(!*error && [syncData.TriggerUpdateDrop length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerUpdateDrop UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
        }
        if(!*error && [syncData.TriggerDeleteDrop length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerDeleteDrop UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
            sqlite3_reset(stmt);
        }
        
        NSArray<SQLiteSyncDataRecord*> *records;
        
        if(!*error){
            records = [syncData SQLiteSyncDataRecordsWithError:error];
        }
        
        if(!*error){
            for(SQLiteSyncDataRecord *record in records){
                if([record.action intValue] == 1 || [record.action intValue] == 2 || [record.action intValue] == 3){
                    NSString *query;
                    
                    switch ([record.action intValue]) {
                        case 1:
                            query = syncData.QueryInsert;
                            break;
                        case 2:
                            query = syncData.QueryUpdate;
                            break;
                        case 3:
                            query = [syncData.QueryDelete stringByAppendingString:@"?"];
                            break;
                    }
                    
                    if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                        for(int i = 0; i < [record.columns count]; i++){
                            sqlite3_bind_text(stmt, i + 1, [[record.columns objectAtIndex:i] UTF8String], -1, SQLITE_TRANSIENT);
                        }
                        
                        if(sqlite3_step(stmt) != SQLITE_DONE){
                            *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                        }
                        
                        sqlite3_reset(stmt);
                    }
                    else{
                        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
                    }
                    
                    if(*error) break;
                }
            }
        }
        
        if(!*error && [syncData.TriggerInsert length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerInsert UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
            sqlite3_reset(stmt);
        }
        if(!*error && [syncData.TriggerUpdate length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerUpdate UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
            sqlite3_reset(stmt);
        }
        if(!*error && [syncData.TriggerDelete length] > 0){
            if(sqlite3_prepare_v2(db, [syncData.TriggerDelete UTF8String], -1, &stmt, NULL) != SQLITE_OK
               || sqlite3_step(stmt) != SQLITE_DONE){
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:@"%s", sqlite3_errmsg(db)] forKey:NSLocalizedDescriptionKey]];
            }
            sqlite3_reset(stmt);
        }
        
        if(*error){
            sqlite3_exec(db, "ROLLBACK TRANSACTION", 0, 0, 0);
        }
        else{
            sqlite3_exec(db, "COMMIT", 0, 0, 0);
        }
        
        if(stmt){
            sqlite3_finalize(stmt);
        }
        
        sqlite3_close(db);
    }
    else{
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:@"Failed to open database" forKey:NSLocalizedDescriptionKey]];
    }
    
    if(*error) return;
    
    [self commitSynchronization:syncData.SyncId error:error];
    
    if(*error) return;
    
}

-(void)commitSynchronization:(NSNumber*)syncId error:(NSError **)error{
    NSString *requestUrlString = [NSString stringWithFormat:@"%@/CommitSync/%@", _serverURL, syncId];
    NSURL *requestURL = [NSURL URLWithString:requestUrlString];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setURL:requestURL];
    [request setHTTPMethod:@"GET"];
    
    NSHTTPURLResponse *response;
    
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:error];
    
    if(!*error){
        switch (response.statusCode) {
            case 200:
                break;
            default:
                *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:[NSDictionary dictionaryWithObject:[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] forKey:NSLocalizedDescriptionKey]];
                break;
        }
    }
}
@end

@implementation SQLiteSyncDataRecord
@end

@implementation SQLiteSyncData
-(NSArray<SQLiteSyncDataRecord*>*)SQLiteSyncDataRecordsWithError:(NSError **)error{
    NSMutableArray<SQLiteSyncDataRecord*> *dataRecords = [NSMutableArray array];
    
    XMLDictionaryParser *parser = [[XMLDictionaryParser alloc] init];
    [parser setCollapseTextNodes:YES];
    [parser setStripEmptyNodes:NO];
    [parser setTrimWhiteSpace:NO];
    [parser setAlwaysUseArrays:YES];
    
    NSNumberFormatter *numFormatter = [[NSNumberFormatter alloc] init];
    numFormatter.numberStyle = NSNumberFormatterDecimalStyle;
    
    @try {
        NSArray *records = [[parser dictionaryWithString:_Records] valueForKeyPath:@"r"];
        
        for(NSDictionary *record in records){
            NSString *action = [record attributes][@"a"];
            NSArray *columns = [record valueForKey:@"c"];
            NSMutableArray<NSString*> *recordColumns = [NSMutableArray array];
            
            for(NSObject *column in columns){
                if([column isKindOfClass:[NSString class]]){
                    [recordColumns addObject:(NSString*)column];
                }
                else if([column isKindOfClass:[NSArray class]]){
                    [recordColumns addObject:[[(NSArray*)column firstObject] innerText]];
                }
                else if([column isKindOfClass:[NSDictionary class]]){
                    if([[(NSDictionary*)column allKeys] count] > 0){
                        [recordColumns addObject:[[[(NSDictionary*)column allValues] firstObject] innerText]];
                    }
                    else{
                        [recordColumns addObject:@""];
                    }
                }
                else{
                    [recordColumns addObject:column.description];
                }
            }
            
            SQLiteSyncDataRecord * dataRecord = [[SQLiteSyncDataRecord alloc] init];
            dataRecord.action = [numFormatter numberFromString:action];
            dataRecord.columns = [recordColumns copy];
            [dataRecords addObject:dataRecord];
        }
    } @catch (NSException *exception) {
        *error = [NSError errorWithDomain:@"com.sqlite-sync" code:0 userInfo:exception.userInfo];
    }
    
    return dataRecords;
}
@end
