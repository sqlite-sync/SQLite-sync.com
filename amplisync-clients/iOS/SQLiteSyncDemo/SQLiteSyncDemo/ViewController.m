//
//  ViewController.m
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import "ViewController.h"
#import "SQLiteSync.h"
#import "TableViewController.h"

@interface ViewController ()
@property (strong,nonatomic) NSArray<NSString*> *tables;
@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    [self pickerTables_initialize];
    [self pickerTables_setDataSource];
    
    NSString *sqlite_sync_url = [[NSUserDefaults standardUserDefaults] stringForKey:@"sqlite_sync_url"];
    [_tb_syncUrl setText:sqlite_sync_url.length > 0 ? sqlite_sync_url : @"https://one-million-demo.ampliapps.com/sync/API3"];
    
    [self.view addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(dismissKeyboard)]];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    if([segue.identifier isEqualToString:@"segue_tableView_Show"]){
        TableViewController *tableView = [segue destinationViewController];
        tableView.tableName = _tb_tableName.text;
    }
}

- (IBAction)bt_reinitialize_Click:(id)sender {
    [_ai_progress startAnimating];
    
    NSString *sqlite_sync_url = _tb_syncUrl.text;
    [[NSUserDefaults standardUserDefaults] setObject:sqlite_sync_url forKey:@"sqlite_sync_url"];
    
    SQLiteSync *sqlite_sync = [[SQLiteSync alloc] initWithDbFileName:@"sqlitesync.db" serverURL:sqlite_sync_url];
    
    [sqlite_sync initializeSubscriber:[self subscriberId] onFinish:^(NSError * _Nullable error) {
        [_ai_progress stopAnimating];
        
        [self pickerTables_setDataSource];
        
        if(error){
            [self showMessage:[NSString stringWithFormat:@"Initialization finished with error: \n%@", [[error.userInfo allValues] firstObject]]];
        }
        else{
            [self showMessage:@"Initialization finished successfully"];
        }
    }];
}

- (IBAction)bt_synchronize_Click:(id)sender {
    [_ai_progress startAnimating];
    
    NSString *sqlite_sync_url = _tb_syncUrl.text;
    [[NSUserDefaults standardUserDefaults] setObject:sqlite_sync_url forKey:@"sqlite_sync_url"];
    
    SQLiteSync *sqlite_sync = [[SQLiteSync alloc] initWithDbFileName:@"sqlitesync.db" serverURL:sqlite_sync_url];
    
    [sqlite_sync synchronizeSubscriber:[self subscriberId] onFinish:^(NSError * _Nullable error) {
        [_ai_progress stopAnimating];
        
        if(error){
            [self showMessage:[NSString stringWithFormat:@"Data synchronization finished with error: \n%@", [[error.userInfo allValues] firstObject]]];
        }
        else{
            [self showMessage:@"Data synchronization finished successfully"];
        }
    }];
}

- (IBAction)bt_selectFrom_Click:(id)sender {
    [self performSegueWithIdentifier:@"segue_tableView_Show" sender:sender];
}

- (void) dismissKeyboard {
    [self.view endEditing:true];
}

-(NSString*)subscriberId{
    //we sugest createing unique subscriber id, example:
    //return [[[UIDevice currentDevice] identifierForVendor] UUIDString];
    //For demo purpose we use static subscriber id
    return @"u90";
}

- (void)showMessage:(NSString*)message{
    [[[UIAlertView alloc] initWithTitle:@"SQLite-sync Demo"
                               message:message
                              delegate:nil
                     cancelButtonTitle:@"OK"
                      otherButtonTitles:nil] show];
}

-(void)pickerTables_initialize{
    UIPickerView *picker = [[UIPickerView alloc]init];
    picker.dataSource = self;
    picker.delegate = self;
    picker.showsSelectionIndicator = YES;
    
    UIBarButtonItem *doneButton = [[UIBarButtonItem alloc]
                                   initWithTitle:@"SELECT * FROM " style:UIBarButtonItemStyleDone
                                   target:self action:@selector(bt_selectFrom_Click:)];
    NSArray *toolbarItems = [NSArray arrayWithObjects: doneButton, nil];
    
    UIToolbar *toolBar = [[UIToolbar alloc]initWithFrame:CGRectMake(0, self.view.frame.size.height - picker.frame.size.height-50, 320, 50)];
    [toolBar setBarStyle:UIBarStyleDefault];
    [toolBar setItems:toolbarItems];
    
    _tb_tableName.inputView = picker;
    _tb_tableName.inputAccessoryView = toolBar;
    _tb_tableName.delegate = self;
}

-(void)pickerTables_setDataSource{
    NSMutableArray<NSString*> *tables = [NSMutableArray array];
    
    sqlite3 *db;
    sqlite3_stmt *stmt = nil;
    NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsDir = [dirPaths objectAtIndex:0];
    NSString *databasePath = [[NSString alloc] initWithString: [docsDir stringByAppendingPathComponent: @"sqlitesync.db"]];
    
    if(sqlite3_open([databasePath UTF8String], &db) == SQLITE_OK){
        NSString *query = @"select tbl_Name from sqlite_master where type='table';";
        if(sqlite3_prepare_v2(db, [query UTF8String], -1, &stmt, NULL) == SQLITE_OK){
            while(sqlite3_step(stmt) == SQLITE_ROW) {
                [tables addObject:[NSString stringWithUTF8String:(char *)sqlite3_column_text(stmt, 0)]];
            }
        }
        sqlite3_finalize(stmt);
        sqlite3_close(db);
    }
    
    _tables = [tables copy];
    
    [(UIPickerView*)_tb_tableName.inputView reloadAllComponents];
}

#pragma mark - Picker View Data source
-(NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView{
    return 1;
}
-(NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component{
    return [_tables count];
}

#pragma mark- Picker View Delegate

-(void)pickerView:(UIPickerView *)pickerView didSelectRow:(NSInteger)row inComponent:(NSInteger)component{
    [_tb_tableName setText:[_tables objectAtIndex:row]];
}
- (NSString *)pickerView:(UIPickerView *)pickerView titleForRow:(NSInteger)row forComponent:(NSInteger)component{
    return [_tables objectAtIndex:row];
}

@end
