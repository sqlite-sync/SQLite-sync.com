//
//  SQLiteSyncDataObject.h
//  iOS Library
//
//  Created by Tomasz Dziemidowicz on 03/01/15.
//
//

#import <Foundation/Foundation.h>
#import "JSONModel.h"

@protocol SQLiteSyncDataObject
@end

@interface SQLiteSyncDataObject : JSONModel

@property (nonatomic, strong) NSString *TableName;
@property (nonatomic, strong) NSString *Records;
@property (nonatomic, strong) NSString *QueryInsert;
@property (nonatomic, strong) NSString *QueryUpdate;
@property (nonatomic, strong) NSString *QueryDelete;

@property (nonatomic, strong) NSString *TriggerInsert;
@property (nonatomic, strong) NSString *TriggerUpdate;
@property (nonatomic, strong) NSString *TriggerDelete;
@property (nonatomic, strong) NSString *TriggerInsertDrop;
@property (nonatomic, strong) NSString *TriggerUpdateDrop;
@property (nonatomic, strong) NSString *TriggerDeleteDrop;

@property (nonatomic, strong) NSNumber *SyncId;

@property (nonatomic, strong) NSString *SQLiteSyncVersion;

+ (NSArray<SQLiteSyncDataObject>*)arrayOfModelsFromString:(NSString*)jsonString;

@end

@interface SQLiteSyncTableChanges : JSONModel
@property (strong, nonatomic) NSArray<SQLiteSyncDataObject>* dataObjects;
@end

