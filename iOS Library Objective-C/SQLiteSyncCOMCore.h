//
//  SQLiteSyncCOMCore.h
//  iOS Library
//
//  Created by Tomasz Dziemidowicz on 30/12/14.
//
//

#import <Foundation/Foundation.h>
#import "DBManager.h"
#import "SQLiteSyncDataObject.h"

@interface SQLiteSyncCOMCore : NSObject{
    DBManager *sqliteDbManager;
}

- (id)initWithDatabaseFilename: (NSString*)dbFilename serviceUrl:(NSString*)serviceUrl;
- (void)reinitializeDB:(NSString *)subscriberId;
- (void)sendAndRecieveChanges:(NSString *)subscriberId;

@end


