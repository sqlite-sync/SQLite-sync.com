//
//  SQLiteSyncDataObject.m
//  iOS Library
//
//  Created by Tomasz Dziemidowicz on 03/01/15.
//
//

#import "SQLiteSyncDataObject.h"

@implementation SQLiteSyncDataObject

+ (NSArray<SQLiteSyncDataObject>*)arrayOfModelsFromString:(NSString*)jsonString{
    NSMutableArray *array = [NSMutableArray array];
    NSData* data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
    if(data != nil){
        array = [NSJSONSerialization JSONObjectWithData:data options:nil error:nil];
    }
    return (NSArray<SQLiteSyncDataObject>*)[SQLiteSyncDataObject arrayOfModelsFromDictionaries:array];
}

@end


@implementation SQLiteSyncTableChanges

@end
