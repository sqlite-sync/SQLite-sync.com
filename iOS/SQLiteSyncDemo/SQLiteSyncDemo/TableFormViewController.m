//
//  TableFormViewController.m
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import "TableFormViewController.h"
#import <sqlite3.h>

@interface TableFormViewController ()
@property (strong, nonatomic) NSArray<TableColumn*> *tableColumns;
@end

@implementation TableFormViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _lb_formTitle.text = [NSString stringWithFormat:@"%@ [%@]", _rowId ? @"UPDATE" : @"INSERT INTO", [_tableName uppercaseString]];
    
    [self loadForm];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (IBAction)bt_Back_Click:(id)sender {
    [self dismissViewControllerAnimated:YES completion:NULL];
}

- (IBAction)bt_Submit_Click:(id)sender {
    NSMutableString *builder = [NSMutableString string];
    
    for(int i = 0; i < _tableColumns.count; i++){
        _tableColumns[i].value = [(UITextField*)_formView.arrangedSubviews[i * 2 + 1] text];
    }
    
    if(_rowId){
        [builder appendFormat:@"UPDATE %@ SET ", _tableName];
        for(TableColumn *tableColumn in _tableColumns){
            if(tableColumn.value.length > 0){
                [builder appendFormat:@"%@=?,", tableColumn.name];
            }
        }
        [builder deleteCharactersInRange:NSMakeRange(builder.length - 1, 1)];
        [builder appendString:@" WHERE RowId=?;"];
    }
    else{
        [builder appendFormat:@"INSERT INTO %@ (", _tableName];
        for(TableColumn *tableColumn in _tableColumns){
            if(tableColumn.value.length > 0){
                [builder appendFormat:@"%@,", tableColumn.name];
            }
        }
        [builder deleteCharactersInRange:NSMakeRange(builder.length - 1, 1)];
        [builder appendString:@") VALUES("];
        for(TableColumn *tableColumn in _tableColumns){
            if(tableColumn.value.length > 0){
                [builder appendString:@"?,"];
            }
        }
        [builder deleteCharactersInRange:NSMakeRange(builder.length - 1, 1)];
        [builder appendString:@");"];
    }
    
    BOOL success;
    sqlite3 *db;
    sqlite3_stmt *stmt = nil;
    NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsDir = [dirPaths objectAtIndex:0];
    NSString *databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: @"sqlitesync.db"]];
    
    if(sqlite3_open([databasePath UTF8String], &db) == SQLITE_OK){
        int res = -1;
        
        if(sqlite3_prepare_v2(db, [builder UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            int index = 0;
            for(TableColumn *tableColumn in _tableColumns){
                if(tableColumn.value.length > 0){
                    const char *value = [[tableColumn.value stringByTrimmingCharactersInSet: [NSCharacterSet whitespaceCharacterSet]] UTF8String];
                    sqlite3_bind_text(stmt, ++index, value, -1, SQLITE_TRANSIENT);
                }
            }
            if(_rowId){
                sqlite3_bind_text(stmt, (int)_tableColumns.count + 1, [_rowId UTF8String], -1, SQLITE_TRANSIENT);
            }
            
            res = sqlite3_step(stmt);
            success = res == SQLITE_DONE;
        }
        
        if(!success){
            [self showMessage:[NSString stringWithFormat:@"sqlite3 error: code:%d, message:%s", res, sqlite3_errmsg(db)]];
        }
        
        sqlite3_finalize(stmt);
        sqlite3_close(db);
    }
    
    if(success){
        [self dismissViewControllerAnimated:YES completion:NULL];
    }
}

-(void)loadForm{
    NSMutableArray<TableColumn*> *tableColumns = [NSMutableArray array];
    
    sqlite3 *db;
    sqlite3_stmt *stmt = nil;
    NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsDir = [dirPaths objectAtIndex:0];
    NSString *databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: @"sqlitesync.db"]];
    
    if(sqlite3_open([databasePath UTF8String], &db) == SQLITE_OK){
        NSString *query = [NSString stringWithFormat:@"PRAGMA table_info(%@);", _tableName];
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            
            int nameIdx = -1;
            int typeIdx = -1;
            
            for(int i = 0; i < sqlite3_column_count(stmt); i++){
                if([[NSString stringWithUTF8String:sqlite3_column_name(stmt, i)] caseInsensitiveCompare:@"name"] == NSOrderedSame){
                    nameIdx = i;
                }
                else if([[NSString stringWithUTF8String:sqlite3_column_name(stmt, i)] caseInsensitiveCompare:@"type"] == NSOrderedSame){
                    typeIdx = i;
                }
                if(nameIdx > 0 && typeIdx > 0){
                    break;
                }
            }
            
            if(nameIdx > 0 && typeIdx > 0){
                while(sqlite3_step(stmt) == SQLITE_ROW) {
                    NSString *name = [NSString stringWithFormat:@"%s", sqlite3_column_text(stmt, nameIdx)];
                    if([name caseInsensitiveCompare:@"RowId"] != NSOrderedSame
                       && [name caseInsensitiveCompare:@"MergeUpdate"] != NSOrderedSame){
                        TableColumn *tableColumn = [[TableColumn alloc] init];
                        tableColumn.name = name;
                        tableColumn.type = [NSString stringWithFormat:@"%s", sqlite3_column_text(stmt, typeIdx)];
                        [tableColumns addObject:tableColumn];
                    }
                }
            }
        }
        
        _tableColumns = [tableColumns copy];
        
        if(_rowId){
            NSMutableString *builder = [NSMutableString string];
            
            [builder appendString:@"SELECT "];
            for(int i = 0; i < _tableColumns.count; i++){
                if(i != 0){
                    [builder appendString:@","];
                }
                [builder appendString:_tableColumns[i].name];
            }
            [builder appendFormat:@" FROM %@ WHERE RowId = ?;", _tableName];
            
            if(sqlite3_prepare_v2(db, [builder UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                sqlite3_bind_text(stmt, 1, [_rowId UTF8String], -1, SQLITE_TRANSIENT);
                
                if(sqlite3_step(stmt) == SQLITE_ROW){
                    for(int i = 0; i < _tableColumns.count; i++){
                        _tableColumns[i].value = [NSString stringWithFormat:@"%s", sqlite3_column_text(stmt, i)];
                    }
                }
            }
        }
        
        sqlite3_finalize(stmt);
        sqlite3_close(db);
    }
    
    for(TableColumn *tableColumn in tableColumns){
        UILabel *label = [[UILabel alloc] init];
        UITextField *textBox = [[UITextField alloc] init];
        
        label.text = tableColumn.name;
        label.textColor = [UIColor whiteColor];
        
        textBox.text = tableColumn.value;
        textBox.backgroundColor = [UIColor whiteColor];
        textBox.borderStyle = UITextBorderStyleRoundedRect;
        if([tableColumn.type caseInsensitiveCompare:@"INTEGER"] == NSOrderedSame){
            textBox.keyboardType = UIKeyboardTypeNumberPad;
        }
        
        [_formView addArrangedSubview:label];
        [_formView addArrangedSubview:textBox];
    }
    
    _scrollView.contentSize = CGSizeMake(_scrollView.contentSize.width, tableColumns.count * 55);
}

- (void)showMessage:(NSString*)message{
    [[[UIAlertView alloc] initWithTitle:@"SQLite-sync Demo"
                                message:message
                               delegate:nil
                      cancelButtonTitle:@"OK"
                      otherButtonTitles:nil] show];
}

@end


@implementation TableColumn
@end
