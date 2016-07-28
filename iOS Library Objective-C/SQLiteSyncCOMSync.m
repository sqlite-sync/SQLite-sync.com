//
//  SQLiteSyncCOMSync.c
//  iOS Library
//
//  Created by Konrad Gardocki on 04.07.2016.
//
//

#include "SQLiteSyncCOMSync.h"

@implementation SQLiteSyncCOMSync

#define startActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:YES]
#define stopActivityIndicator  [[UIApplication sharedApplication]setNetworkActivityIndicatorVisible:NO];

NSString *_serverUrl;

-(id)initWithServiceUrl:(NSString*)serviceUrl
{
    self = [super init];
    _serverUrl = serviceUrl;
    return self;
}

-(NSString*)ReceiveData:(NSString*)subscriberId data:(NSString *)data {
    return [self createRequestForAction:@"ReceiveData" SOAPBody:[NSString stringWithFormat:@"<subscriber>%@</subscriber><data>%@</data>", subscriberId, [self xmlSimpleEscape:data]]];
}

-(NSString*)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName{
    return [self createRequestForAction:@"GetDataForSync" SOAPBody:[NSString stringWithFormat:@"<subscriber>\"%@\"</subscriber><table>\"%@\"</table>", subscriberId, [self xmlSimpleEscape:tableName]]];
}

-(NSString*)SyncCompleted:(long)syncId{
    return [self createRequestForAction:@"SyncCompleted" SOAPBody:[NSString stringWithFormat:@"<syncId>%ld</syncId>", syncId]];
}

-(NSString*)GetFullDBSchema:(NSString*)subscriberId {
    return [self createRequestForAction:@"GetFullDBSchema" SOAPBody:[NSString stringWithFormat:@"<subscriber>\"%@\"</subscriber>", subscriberId]];
}

-(NSString*)createRequestForAction:(NSString*)SOAPAction SOAPBody:(NSString*)SOAPBody {
    NSString *soapFormat = [NSString stringWithFormat:@"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            "<soap:Body>\n"
                            "<%@ xmlns=\"http://sqlite-sync.com/\">\n"
                            "%@\n"
                            "</%@>\n"
                            "</soap:Body>\n"
                            "</soap:Envelope>",SOAPAction,SOAPBody,SOAPAction];
    
    NSURL *locationOfWebService = [NSURL URLWithString:_serverUrl];
    NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc]initWithURL:locationOfWebService];
    NSString *msgLength = [NSString stringWithFormat:@"%lu",(unsigned long)[soapFormat length]];
    [theRequest addValue:@"text/xml" forHTTPHeaderField:@"Content-Type"];
    [theRequest addValue:[NSString stringWithFormat:@"http://sqlite-sync.com/%@", SOAPAction] forHTTPHeaderField:@"SOAPAction"];
    [theRequest addValue:msgLength forHTTPHeaderField:@"Content-Length"];
    [theRequest setHTTPMethod:@"POST"];
    [theRequest setHTTPBody:[soapFormat dataUsingEncoding:NSUTF8StringEncoding]];
    
    NSDictionary *returnData;
    NSURLResponse* response;
    NSError* error = nil;
    
    startActivityIndicator;
    NSData* result = [NSURLConnection sendSynchronousRequest:theRequest returningResponse:&response error:&error];
    stopActivityIndicator;
    
    if(error != nil){
        NSLog(@"SQLiteSyncCOMSync Error: %@",error.localizedDescription);
    }
    else{
        NSString *theXML = [[NSString alloc] initWithBytes: [result bytes] length:[result length] encoding:NSUTF8StringEncoding];
        returnData=[[NSDictionary alloc]initWithDictionary:[XMLReader dictionaryForXMLString:theXML error:&error]];
    }
    if(error != nil){
        NSLog(@"SQLiteSyncCOMSync Error: %@",error.localizedDescription);
    }
    
    return [returnData valueForKeyPath:[NSString stringWithFormat:@"soap:Envelope.soap:Body.%@Response.%@Result.innerValue", SOAPAction,SOAPAction]];
}

- (NSString *)xmlSimpleEscape:(NSString *)value
{
    NSMutableString *mutableValue = [NSMutableString stringWithString:value];
    [mutableValue replaceOccurrencesOfString:@"&"  withString:@"&amp;"  options:NSLiteralSearch range:NSMakeRange(0, [mutableValue length])];
    [mutableValue replaceOccurrencesOfString:@"\"" withString:@"&quot;" options:NSLiteralSearch range:NSMakeRange(0, [mutableValue length])];
    [mutableValue replaceOccurrencesOfString:@"'"  withString:@"&#x27;" options:NSLiteralSearch range:NSMakeRange(0, [mutableValue length])];
    [mutableValue replaceOccurrencesOfString:@">"  withString:@"&gt;"   options:NSLiteralSearch range:NSMakeRange(0, [mutableValue length])];
    [mutableValue replaceOccurrencesOfString:@"<"  withString:@"&lt;"   options:NSLiteralSearch range:NSMakeRange(0, [mutableValue length])];
    
    return mutableValue;
}

@end
