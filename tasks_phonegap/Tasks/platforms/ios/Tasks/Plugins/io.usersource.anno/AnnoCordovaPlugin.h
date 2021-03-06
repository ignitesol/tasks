//
//  AnnoCordovaPlugin.h
//  UserSource
//
//  Created by Imran Ahmed on 11/04/14.
//
//

#import <Cordova/CDV.h>
#import "AppDelegate.h"
#import "AnnoDrawViewController.h"
#import "CommunityViewController.h"
#import "IntroViewController.h"
#import "OptionFeedbackViewController.h"
#import "AnnoUtils.h"
#import "ScreenshotGestureListener.h"

@interface AnnoCordovaPlugin : CDVPlugin {}

extern AnnoUtils *annoUtils;
extern ScreenshotGestureListener *screenshotGestureListener;
extern AppDelegate *appDelegate;

- (void) exit_current_activity:(CDVInvokedUrlCommand*)command;
- (void) show_toast:(CDVInvokedUrlCommand*)command;
- (void) goto_anno_home:(CDVInvokedUrlCommand*)command;
- (void) start_activity:(CDVInvokedUrlCommand*)command;
- (void) process_image_and_appinfo:(CDVInvokedUrlCommand*)command;
- (void) start_anno_draw:(CDVInvokedUrlCommand*)command;
- (void) get_screenshot_path:(CDVInvokedUrlCommand*)command;
- (void) get_anno_screenshot_path:(CDVInvokedUrlCommand*)command;
- (void) show_softkeyboard:(CDVInvokedUrlCommand*)command;
- (void) close_softkeyboard:(CDVInvokedUrlCommand*)command;
- (void) exit_intro:(CDVInvokedUrlCommand*)command;
- (void) get_recent_applist:(CDVInvokedUrlCommand*)command;
- (void) get_installed_app_list:(CDVInvokedUrlCommand*)command;
- (void) enable_native_gesture_listener:(CDVInvokedUrlCommand*)command;
- (void) trigger_create_anno:(CDVInvokedUrlCommand*)command;

- (void) showCommunityPage;
- (void) ShowIntroPage;
- (void) showOptionFeedback;
+ (void) showAnnoDraw:(NSString*)imageURI levelValue:(int)levelValue;

@end
