//
//  TableFormViewController.h
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface TableFormViewController : UIViewController
@property (strong, nonatomic) NSString * _Nonnull tableName;
@property (strong, nonatomic) NSString * _Nullable rowId;
@property (weak, nonatomic) IBOutlet UILabel * _Nullable lb_formTitle;
@property (weak, nonatomic) IBOutlet UIScrollView * _Nullable scrollView;
@property (weak, nonatomic) IBOutlet UIStackView * _Nullable formView;
@end

@interface TableColumn : NSObject
@property (nonatomic, strong) NSString * _Nonnull name;
@property (nonatomic, strong) NSString * _Nonnull type;
@property (nonatomic, strong) NSString * _Nullable value;
@end
