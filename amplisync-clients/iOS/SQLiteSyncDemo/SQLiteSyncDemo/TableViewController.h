//
//  TableViewController.h
//  SQLiteSyncDemo
//
//  Copyright © 2017 sqlite-sync.com. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface TableViewController : UIViewController <UICollectionViewDelegate,UICollectionViewDataSource,UIAlertViewDelegate>
@property (weak, nonatomic) IBOutlet UIScrollView *scrollView;
@property (weak, nonatomic) IBOutlet UICollectionView *collectionView;
@property (weak, nonatomic) IBOutlet UILabel *lb_tableName;
@property (strong, nonatomic) NSString *tableName;
@end
