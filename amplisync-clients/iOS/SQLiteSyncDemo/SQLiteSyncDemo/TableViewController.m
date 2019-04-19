//
//  TableViewController.m
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import "TableViewController.h"
#import "TableCell.h"
#import "TableButtonsCell.h"
#import "TableFormViewController.h"
#import <sqlite3.h>

@interface TableViewController ()
@property (strong, nonatomic) NSArray<NSString*> *columns;
@property (strong, nonatomic) NSArray<NSArray<NSString*>*> *rows;
@end

@implementation TableViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _lb_tableName.text = [_tableName uppercaseString];
}

-(void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    
    [self getDataSource];
    [self.collectionView reloadData];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)viewDidLayoutSubviews{
    long tableCollectionViewFrameSizeWidth = ([_columns count] + 2) * 100;
    
    _collectionView.frame = CGRectMake(
        _collectionView.frame.origin.x,
        _collectionView.frame.origin.y,
        tableCollectionViewFrameSizeWidth,
        _collectionView.frame.size.height);
    _scrollView.contentSize = _collectionView.frame.size;
}

#pragma mark - Navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    if([segue.identifier isEqualToString:@"segue_tableForm_Show"]){
        TableFormViewController *form = [segue destinationViewController];
        form.tableName = _tableName;
        if(sender && [sender isKindOfClass:[NSString class]]){
            form.rowId = sender;
        }
    }
}

#pragma mark - Collection View Delegate
- (NSInteger)numberOfSectionsInCollectionView:(UICollectionView *)collectionView {
    return 1;
}

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section{
    return ([_columns count] + 1) * ([_rows count] + 1);
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath{
    NSUInteger rowIndex = indexPath.row / ([_columns count] + 1);
    NSUInteger columnIndex = indexPath.row % ([_columns count] + 1);
    
    if(rowIndex == 0) {
        if(columnIndex == 0) {
            TableCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"TableCell" forIndexPath:indexPath];
            cell.lb_CellText.text = @"";
            return cell;
        }
        else{
            TableCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"TableCell" forIndexPath:indexPath];
            cell.lb_CellText.text = _columns[columnIndex - 1];
            cell.lb_CellText.font = [UIFont boldSystemFontOfSize:16];
            return cell;
        }
    }
    else{
        if(columnIndex == 0) {
            TableButtonsCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"TableButtonsCell" forIndexPath:indexPath];
            cell.btEdit.tag = rowIndex - 1;
            [cell.btEdit addTarget:self action:@selector(btEdit_Click:) forControlEvents:UIControlEventTouchUpInside];
            cell.btDelete.tag = rowIndex - 1;
            [cell.btDelete addTarget:self action:@selector(btDelete_Click:) forControlEvents:UIControlEventTouchUpInside];
            return cell;
        }
        else{
            TableCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"TableCell" forIndexPath:indexPath];
            cell.lb_CellText.text = _rows[rowIndex -1][columnIndex - 1];
            return cell;
        }
    }
}



-(void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{
    if(buttonIndex == 1){
        for(int i = 0; i < _columns.count; i++){
            if([_columns[i] caseInsensitiveCompare:@"RowId"] == NSOrderedSame){
                NSString *rowId = _rows[alertView.tag][i];
                
                sqlite3 *db;
                sqlite3_stmt *stmt = nil;
                NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
                NSString *docsDir = [dirPaths objectAtIndex:0];
                NSString *databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: @"sqlitesync.db"]];
                
                if(sqlite3_open([databasePath UTF8String], &db) == SQLITE_OK){
                    NSString *query = [NSString stringWithFormat:@"DELETE FROM %@ WHERE RowId=?", _tableName];
                    if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
                        sqlite3_bind_text(stmt, 1, [rowId UTF8String], -1, SQLITE_TRANSIENT);
                        sqlite3_step(stmt);
                    }
                    sqlite3_finalize(stmt);
                    sqlite3_close(db);
                }
                
                [self getDataSource];
                [self.collectionView reloadData];
                
                break;
            }
        }
    }
}

- (IBAction)btAdd_Click:(id)sender {
    [self performSegueWithIdentifier:@"segue_tableForm_Show" sender:NULL];
}

- (IBAction)btEdit_Click:(UIButton*)sender {
    for(int i = 0; i < _columns.count; i++){
        if([_columns[i] caseInsensitiveCompare:@"RowId"] == NSOrderedSame){
            NSString *rowId = _rows[sender.tag][i];
            [self performSegueWithIdentifier:@"segue_tableForm_Show" sender:rowId];
            break;
        }
    }
}

- (IBAction)btBack_Click:(id)sender {
    [self dismissViewControllerAnimated:YES completion:NULL];
}

- (IBAction)btDelete_Click:(UIButton*)sender {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle:@"Do you really want to delete this record?"
                          message:nil
                          delegate:self
                          cancelButtonTitle:@"No"
                          otherButtonTitles:@"Yes", nil];
    [alert setTag:sender.tag];
    [alert show];
}

-(void)getDataSource{
    NSMutableArray<NSString*> *columns = [NSMutableArray array];
    NSMutableArray<NSArray<NSString*>*> *rows = [NSMutableArray array];
    
    sqlite3 *db;
    sqlite3_stmt *stmt = nil;
    NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsDir = [dirPaths objectAtIndex:0];
    NSString *databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: @"sqlitesync.db"]];
    
    if(sqlite3_open([databasePath UTF8String], &db) == SQLITE_OK){
        NSString *query = [NSString stringWithFormat:@"Select * FROM %@", _tableName];
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            int column_count = sqlite3_column_count(stmt);
            
            for(int i = 0; i < column_count; i++){
                [columns addObject:[NSString stringWithUTF8String:sqlite3_column_name(stmt, i)]];
            }
            
            while(sqlite3_step(stmt) == SQLITE_ROW) {
                NSMutableArray<NSString*> *row = [NSMutableArray array];
                for(int i = 0; i < column_count; i++){
                    [row addObject:[NSString stringWithFormat:@"%s", sqlite3_column_text(stmt, i)]];
                }
                [rows addObject:[row copy]];
            }
        }
        sqlite3_finalize(stmt);
        sqlite3_close(db);
    }
    
    _columns = [columns copy];
    _rows = [rows copy];
}
@end
