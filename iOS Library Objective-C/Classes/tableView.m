//
//  tableView.m
//  iOS Library
//
//  Created by Konrad Gardocki on 22.07.2016.
//
//

#import <Foundation/Foundation.h>
#import "tableView.h"
#import "DBManager.h"


@interface tableView ()

@end

@implementation tableView

NSString *_tableName;
NSArray *_tableColumns;
NSArray *_tableResult;

+ (tableView*) initWithTableName:(NSString*)tableName{
    tableView *view = [[tableView alloc]initWithNibName:@"tableView" bundle:nil];
    view.tableName = tableName.uppercaseString;
    return view;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [lbTableName setText:_tableName];
    [self.tableCollectionView registerNib:[UINib nibWithNibName:@"MyCell" bundle:nil] forCellWithReuseIdentifier:@"CELL"];
    
    DBManager * db = [[DBManager alloc] initWithDatabaseFilename:@"sqlitesync.db"];
    _tableResult = [db loadDataFromDB:[NSString stringWithFormat:@"SELECT * FROM %@;",_tableName]];
    _tableColumns = [db arrColumnNames];
}

- (void)viewDidLayoutSubviews {
    long _tableCollectionViewFrameSizeWidth = [_tableColumns count] * 101;
    
    self.tableCollectionView.frame = CGRectMake(
                                                _tableCollectionView.frame.origin.x,
                                                _tableCollectionView.frame.origin.y,
                                                _tableCollectionViewFrameSizeWidth,
                                                _tableCollectionView.frame.size.height);
    self.tableScrollView.contentSize = self.tableCollectionView.frame.size;
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (void)viewDidUnload {
    [super viewDidUnload];
}

- (void)dealloc {
    [lbTableName release];
    [_tableCollectionView release];
    [_tableScrollView release];
    [super dealloc];
}
- (IBAction)btBackAction:(id)sender {
    [self dismissViewControllerAnimated:YES completion:NULL];
}

- (NSInteger)numberOfSectionsInCollectionView:(UICollectionView *)collectionView {
    return 1;
}

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section{
    if (_tableResult == nil)
        return 0;
    else{
         return [_tableColumns count] * ([_tableResult count] + 1);
    }
}

- (MyCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath{
    MyCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"CELL" forIndexPath:indexPath];
    cell.cellLabel.text = [NSString stringWithFormat:@"cell %li",(long)indexPath.row];
    
    long i = indexPath.row;
    long columnsCount = _tableColumns.count;
    long rowIndex = i / columnsCount;
    long columnIndex = i % columnsCount;
    
    if(rowIndex == 0) {
        cell.cellLabel.text = [NSString stringWithFormat:@"%@",_tableColumns[columnIndex]];
        [cell.cellLabel setFont:[UIFont boldSystemFontOfSize:16]];
    }
    else{
        cell.cellLabel.text = [NSString stringWithFormat:@"%@",_tableResult[rowIndex-1][columnIndex]];
    }
    
    return cell;
}
@end