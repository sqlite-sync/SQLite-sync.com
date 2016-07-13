//
//  DBManager.h
//  SQLite3DBSample
//
//  Created by Gabriel Theodoropoulos on 25/6/14.
//  Copyright (c) 2014 Appcoda. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DBManager : NSObject

@property (nonatomic, strong) NSMutableArray *arrColumnNames;
@property (nonatomic) int affectedRows;
@property (nonatomic) long long lastInsertedRowID;

-(instancetype)initWithDatabaseFilename:(NSString *)dbFilename;

-(NSArray *)loadDataFromDB:(NSString *)query;
-(void)executeNonQuery:(NSString *)query;
-(void)executeNonQuery:(NSString *)query parameters:(NSArray *)parameters;

-(void)BeginTransaction;
-(void)Commit;
-(void)Rollback;
@end
