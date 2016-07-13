//
//  myView.h
//  W3WebService
//
//  Created by Ravi Dixit on 4/15/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface myView : UIViewController{
	IBOutlet UITextField *txt1,*output;
}
@property (nonatomic,retain) UITextField *txt1,*output;
-(IBAction)backGroundTap:(id)sender;
-(IBAction)sendAndRecieveChangesTap:(id)sender;
-(IBAction)invokeService;

@end
