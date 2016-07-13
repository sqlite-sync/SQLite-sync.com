//
//  SQLiteSyncCOMSync.h
//  iOS Library
//
//  Created by Konrad Gardocki on 04.07.2016.
//
//

#import <Foundation/Foundation.h>
#import "SQLiteSyncCOMSync.h"
#import "XMLReader.h"

@interface SQLiteSyncCOMSync : NSObject{
    NSMutableData *webData;
}
-(id)initWithServiceUrl:(NSString*)serverUrl;
-(void)ReceiveData:(NSString*)subscriberId data:(NSString *)data;
-(void)ReceiveData:(NSString*)subscriberId data:(NSString *)data completion:(void (^)(NSDictionary* responseXML))completionHandler;
-(NSDictionary*)ReceiveDataSynch:(NSString*)subscriberId data:(NSString *)data;
-(void)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName;
-(void)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName completion:(void (^)(NSDictionary* responseXML))completionHandler;
-(NSDictionary*)GetDataForSyncSynch:(NSString*)subscriberId tableName:(NSString *)tableName;
-(void)SyncCompleted: (long)syncId;
-(void)SyncCompleted: (long)syncId completion:(void (^)(NSDictionary* responseXML))completionHandler;
-(NSDictionary*)SyncCompletedSynch:(long)syncId;
-(void)GetFullDBSchema:(NSString*)subscriberId;
-(void)GetFullDBSchema:(NSString*)subscriberId completion:(void (^)(NSDictionary* responseXML))completionHandler;
-(NSDictionary*)GetFullDBSchemaSynch:(NSString*)subscriberId;

@end

@interface CompletionHandler:NSObject
@property(nonatomic, retain) NSURLConnection *connection;
@property(nonatomic, copy) void (^handler)(NSDictionary *responseXML);
@end