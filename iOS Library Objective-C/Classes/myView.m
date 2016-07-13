//
//  myView.m
//  W3WebService
//
//  Created by Ravi Dixit on 4/15/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#import "myView.h"
#import "DBManager.h"
#import "JSONModel.h"
#import "JSONHTTPClient.h"
#import "SQLiteSyncCOMCore.h"

@interface myView ()

@end


@implementation myView
@synthesize txt1,output;

#define startActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:YES]
#define stopActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:NO];


- (void)viewDidLoad {
    // Initialize the dbManager property.
    [super viewDidLoad];
    
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
        SQLiteSyncCOMCore *sqliteSyncCOMCore = [[SQLiteSyncCOMCore alloc]initWithDatabaseFilename:@"sqlitesync.db" serviceUrl:@"http://test.sqlite-sync.com/sync.asmx"];
        [sqliteSyncCOMCore reinitializeDB:[NSString stringWithFormat:@"%@", txt1.text]];
        
		[txt1 resignFirstResponder];
        
        
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"SQLite-sync.com sample"
                                                        message:@"Database reinitialization done!"
                                                       delegate:nil
                                              cancelButtonTitle:@"OK"
                                              otherButtonTitles:nil];
        [alert show];
        [alert release];
	}

}

-(IBAction)backGroundTap:(id)sender
{
	[txt1 resignFirstResponder];
}

-(IBAction)sendAndRecieveChangesTap:(id)sender{
    NSLog(@"send and recieve changes");
    
    SQLiteSyncCOMCore *sqliteSyncCOMCore = [[SQLiteSyncCOMCore alloc]initWithDatabaseFilename:@"sqlitesync.db" serviceUrl:@"http://test.sqlite-sync.com/sync.asmx"];
    [sqliteSyncCOMCore sendAndRecieveChanges:[NSString stringWithFormat:@"%@", txt1.text]];
    
    
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"SQLite-sync.com sample"
                                                    message:@"Send and recieve changes done!"
                                                   delegate:nil
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
    [alert release];
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
    [super dealloc];
}


@end
