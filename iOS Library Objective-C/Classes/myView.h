//
//  myView.h
//  W3WebService
//
//  Created by Ravi Dixit on 4/15/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface myView : UIViewController
<UITextFieldDelegate,UIPickerViewDataSource,UIPickerViewDelegate>{
    IBOutlet UITextField *txt1,*output;
    UIPickerView *myPickerView;
    NSArray *pickerArray;
    IBOutlet UITextField *myPickerTextField;
}
@property (nonatomic,retain) UITextField *txt1,*output;
-(IBAction)sendAndRecieveChangesTap:(id)sender;
-(IBAction)invokeService;

@end
