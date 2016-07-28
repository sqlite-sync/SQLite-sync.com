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
}
-(id)initWithServiceUrl:(NSString*)serverUrl;
-(NSString*)ReceiveData:(NSString*)subscriberId data:(NSString *)data;
-(NSString*)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName;
-(NSString*)SyncCompleted:(long)syncId;
-(NSString*)GetFullDBSchema:(NSString*)subscriberId;

@end