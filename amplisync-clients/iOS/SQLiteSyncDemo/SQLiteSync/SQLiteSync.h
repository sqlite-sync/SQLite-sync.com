//
//  SQLiteSync.h
//
//  Created by sqlite-sync.com
//

#import <Foundation/Foundation.h>
#import <sqlite3.h>
#import "JSONModel.h"
#import "XMLDictionary.h"

@interface SQLiteSync : NSObject

/**
 Create instance of SQLiteSync class

 @param dbFileName file name of local sqlite database file
 @param serverURL synchronization webservice url
 @return instance of SQLiteSync class
 */
-(nonnull instancetype)initWithDbFileName:(nonnull NSString*)dbFileName serverURL:(nonnull NSString*)serverURL;

/**
 Asynchronously recreate database schema from remote server for specific subscriber

 @param subscriberId id of subscriber
 @param onFinish callback to be invoke after function complete (check *error if completed succesful)
 */
-(void)initializeSubscriber:(nonnull NSString*)subscriberId onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish;

/**
 Asynchronously send and receive any changes made for tables included in synchronization

 @param subscriberId id of subscriber
 @param onFinish callback to be invoke after function complete (check *error if completed succesful)
 */
-(void)synchronizeSubscriber:(nonnull NSString*)subscriberId onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish;

/**
 Asynchronously add table to synchronization

 @param tableName table name
 @param onFinish callback to be invoke after function complete (check *error if completed succesful)
 */
-(void)addSynchrnizedTable:(nonnull NSString*)tableName onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish;

/**
 Asynchronously remove table from synchronization

 @param tableName table name
 @param onFinish callback to be invoke after function complete (check *error if completed succesful)
 */
-(void)removeSynchrnizedTable:(nonnull NSString*)tableName onFinish:(nonnull void(^)(NSError * _Nullable error))onFinish;

/**
 Recreate database schema from remote server for specific subscriber

 @param subscriberId id of subscriber
 @param error Out parameter used if an error occurs while processing action.
 */
-(void)initializeSubscriber:(nonnull NSString*)subscriberId error:(NSError * _Nullable * _Nullable)error;

/**
 Send and receive any changes made for tables included in synchronization

 @param subscriberId id of subscriber
 @param error Out parameter used if an error occurs while processing action.
 */
-(void)synchronizeSubscriber:(nonnull NSString*)subscriberId error:(NSError * _Nullable * _Nullable)error;

/**
 Add table to synchronization

 @param tableName table name
 @param error Out parameter used if an error occurs while processing action.
 */
-(void)addSynchrnizedTable:(nonnull NSString*)tableName error:(NSError * _Nullable * _Nullable)error;

/**
 Remove table from synchronization

 @param tableName table name
 @param error Out parameter used if an error occurs while processing action.
 */
-(void)removeSynchrnizedTable:(nonnull NSString*)tableName error:(NSError * _Nullable * _Nullable)error;
@end

/**
 Class representing data structure of record changes received from remote server
 */
@interface SQLiteSyncDataRecord : NSObject
@property (nonatomic, strong) NSNumber * _Nullable action;
@property (nonatomic, strong) NSArray<NSString * > * _Nullable columns;
@end

/**
 Class representing data structure of table changes received from remote server
 */
@interface SQLiteSyncData : JSONModel
@property (nonatomic, strong)  NSString * _Nullable TableName;
@property (nonatomic, strong) NSString * _Nullable Records;

@property (nonatomic, strong) NSString * _Nullable QueryInsert;
@property (nonatomic, strong) NSString * _Nullable QueryUpdate;
@property (nonatomic, strong) NSString * _Nullable QueryDelete;

@property (nonatomic, strong) NSString * _Nullable TriggerInsert;
@property (nonatomic, strong) NSString * _Nullable TriggerUpdate;
@property (nonatomic, strong) NSString * _Nullable TriggerDelete;

@property (nonatomic, strong) NSString * _Nullable TriggerInsertDrop;
@property (nonatomic, strong) NSString * _Nullable TriggerUpdateDrop;
@property (nonatomic, strong) NSString * _Nullable TriggerDeleteDrop;

@property (nonatomic, strong) NSNumber * _Nullable SyncId;
@property (nonatomic, strong) NSString * _Nullable SQLiteSyncVersion;

@property (nonatomic, strong) NSNumber * _Nullable MaxPackageSize;
@property (nonatomic, strong) NSNumber * _Nullable RowsCount;

-(NSArray<SQLiteSyncDataRecord*> * _Nonnull)SQLiteSyncDataRecordsWithError:(NSError * _Nullable * _Nullable)error;
@end
