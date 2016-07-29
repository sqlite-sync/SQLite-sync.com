//
//  myView.m
//  W3WebService
//
//  Created by Ravi Dixit on 4/15/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#import "myView.h"
#import "tableView.h"
#import "SQLiteSyncCOMCore.h"

@interface myView ()

@end


@implementation myView
@synthesize txt1,output;

#define startActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:YES]
#define stopActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:NO];


- (void)viewDidLoad {
    [super viewDidLoad];
    [self setTablesDropDown];
    
    [self.view addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(hideKeyboard)]];
}
 

 // The designated initializer.  Override if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if ((self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil])) {
        // Custom initialization
    }
    return self;
}

-(IBAction)invokeService
{
	if ([txt1.text length]==0) {
		
		UIAlertView *alert = [[UIAlertView alloc]initWithTitle:@"WebService"
                                                       message:@"Supply Data in text field"
                                                      delegate:nil
                                             cancelButtonTitle:nil
                                             otherButtonTitles:@"ok",nil];
		[alert show];
		[alert release];
	}
    else {
        SQLiteSyncCOMCore *sqliteSyncCOMCore = [[SQLiteSyncCOMCore alloc]initWithDatabaseFilename:@"sqlitesync.db" serviceUrl:@"http://demo.sqlite-sync.com/sync.asmx"];
        [sqliteSyncCOMCore reinitializeDB:[NSString stringWithFormat:@"%@", txt1.text]];
        
		[txt1 resignFirstResponder];
        
        [self showMessage:@"Reinitialize database done"];        
	}

}

- (void) hideKeyboard {
    [txt1 resignFirstResponder];
    [myPickerTextField resignFirstResponder];
}

-(IBAction)sendAndRecieveChangesTap:(id)sender{
    SQLiteSyncCOMCore *sqliteSyncCOMCore = [[SQLiteSyncCOMCore alloc]initWithDatabaseFilename:@"sqlitesync.db" serviceUrl:@"http://demo.sqlite-sync.com/sync.asmx"];
    [sqliteSyncCOMCore sendAndRecieveChanges:[NSString stringWithFormat:@"%@", txt1.text]];
    
    [self showMessage:@"Send and receive changes done"];
}

- (void)didReceiveMemoryWarning {
    // Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
    
    // Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}


- (void)dealloc {
	[txt1 release];
	[output release];
    [myPickerTextField release];
    [super dealloc];
}

- (void)showMessage:(NSString*)message{
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"SQLite-sync DEMO"
                                                    message:message
                                                   delegate:nil
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
    [alert release];
}

- (void)setTablesDropDown{
    pickerArray = [[NSArray alloc] initWithObjects:@"Documents",@"Entities",@"Users",@"UserDocuments",nil];
    
    myPickerTextField.textAlignment = UITextAlignmentCenter;
    myPickerTextField.delegate = self;
    myPickerView = [[UIPickerView alloc]init];
    myPickerView.dataSource = self;
    myPickerView.delegate = self;
    myPickerView.showsSelectionIndicator = YES;
    UIBarButtonItem *doneButton = [[UIBarButtonItem alloc]
                                   initWithTitle:@"Pick" style:UIBarButtonItemStyleDone
                                   target:self action:@selector(buttonDone:)];
    UIToolbar *toolBar = [[UIToolbar alloc]initWithFrame:
                          CGRectMake(0, self.view.frame.size.height-
                                     myPickerView.frame.size.height-50, 320, 50)];
    [toolBar setBarStyle:UIBarStyleBlackOpaque];
    NSArray *toolbarItems = [NSArray arrayWithObjects: doneButton, nil];
    [toolBar setItems:toolbarItems];
    myPickerTextField.inputView = myPickerView;
    myPickerTextField.inputAccessoryView = toolBar;
}

-(void)buttonDone:(id)sender{
    [myPickerTextField resignFirstResponder];
    tableView *view = [tableView initWithTableName:[myPickerTextField text]];
    [self presentViewController:view animated:YES completion:NULL];
}

#pragma mark - Text field delegates

-(void)textFieldDidBeginEditing:(UITextField *)textField{
}
#pragma mark - Picker View Data source
-(NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView{
    return 1;
}
-(NSInteger)pickerView:(UIPickerView *)pickerView
numberOfRowsInComponent:(NSInteger)component{
    return [pickerArray count];
}

#pragma mark- Picker View Delegate

-(void)pickerView:(UIPickerView *)pickerView didSelectRow:
(NSInteger)row inComponent:(NSInteger)component{
    [myPickerTextField setText:[pickerArray objectAtIndex:row]];
}
- (NSString *)pickerView:(UIPickerView *)pickerView titleForRow:
(NSInteger)row forComponent:(NSInteger)component{
    return [pickerArray objectAtIndex:row];
}


@end
