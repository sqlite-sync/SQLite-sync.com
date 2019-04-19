//
//  ViewController.h
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ViewController : UIViewController <UITextFieldDelegate,UIPickerViewDataSource,UIPickerViewDelegate>

@property (weak, nonatomic) IBOutlet UITextField *tb_syncUrl;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *ai_progress;
@property (weak, nonatomic) IBOutlet UITextField *tb_tableName;

@end

