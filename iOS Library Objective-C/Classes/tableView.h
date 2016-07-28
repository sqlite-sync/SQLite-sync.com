//
//  tableView.h
//  iOS Library
//
//  Created by Konrad Gardocki on 22.07.2016.
//
//

#import <UIKit/UIKit.h>
#import "MyCell.h"

@interface tableView : UIViewController
<UICollectionViewDelegate,UICollectionViewDataSource>{
    
    IBOutlet UILabel *lbTableName;
}

@property (nonatomic, strong) NSString *tableName;

- (IBAction)btBackAction:(id)sender;
+ (tableView*) initWithTableName:(NSString*)tableName;
@property (retain, nonatomic) IBOutlet UICollectionView *tableCollectionView;
@property (retain, nonatomic) IBOutlet UIScrollView *tableScrollView;

@end
