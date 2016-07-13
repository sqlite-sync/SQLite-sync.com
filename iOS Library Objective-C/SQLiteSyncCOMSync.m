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
NSMutableArray<CompletionHandler*> *_completionHandlers;

-(id)initWithServiceUrl:(NSString*)serviceUrl
{
    self = [super init];
    webData = [[NSMutableData alloc]init];
    _serverUrl = serviceUrl;
    _completionHandlers = [NSMutableArray<CompletionHandler*> new];
    return self;
}

-(NSDictionary*)ReceiveDataSynch:(NSString*)subscriberId data:(NSString *)data {
    return [self createRequest:[self getRequestReceiveData:subscriberId data:data] completion:nil async:NO];
}

-(void)ReceiveData:(NSString*)subscriberId data:(NSString *)data{
    [self createRequest:[self getRequestReceiveData:subscriberId data:data] completion:nil async:YES];
}

-(void)ReceiveData:(NSString*)subscriberId data:(NSString *)data completion:(void (^)(NSDictionary* responseXML))completionHandler {
    [self createRequest:[self getRequestReceiveData:subscriberId data:data] completion:completionHandler async:YES];
}

-(NSDictionary*)GetDataForSyncSynch:(NSString*)subscriberId tableName:(NSString *)tableName{
    return [self createRequest:[self getRequestGetDataForSync:subscriberId tableName:tableName] completion:nil async:NO];
}
-(void)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName{
    [self createRequest:[self getRequestGetDataForSync:subscriberId tableName:tableName] completion:nil async:YES];
}
-(void)GetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName completion:(void (^)(NSDictionary* responseXML))completionHandler{
    [self createRequest:[self getRequestGetDataForSync:subscriberId tableName:tableName] completion:completionHandler async:YES];
}

-(NSDictionary*)SyncCompletedSynch:(long)syncId{
    return [self createRequest:[self getRequestSyncCompleted:syncId] completion:nil async:NO];
}
-(void)SyncCompleted:(long)syncId{
    [self createRequest:[self getRequestSyncCompleted:syncId] completion:nil async:YES];
}
-(void)SyncCompleted:(long)syncId completion:(void (^)(NSDictionary* responseXML))completionHandler{
    [self createRequest:[self getRequestSyncCompleted:syncId] completion:completionHandler async:YES];
}

-(NSDictionary*)GetFullDBSchemaSynch:(NSString*)subscriberId {
    return [self createRequest:[self getRequestGetFullDBSchema:subscriberId] completion:nil async:NO];
}
-(void)GetFullDBSchema:(NSString*)subscriberId{
    [self createRequest:[self getRequestGetFullDBSchema:subscriberId] completion:nil async:YES];
}
-(void)GetFullDBSchema:(NSString*)subscriberId completion:(void (^)(NSDictionary* responseXML))completionHandler{
    [self createRequest:[self getRequestGetFullDBSchema:subscriberId] completion:completionHandler async:YES];
}

-(void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    [webData setLength: 0];
}
-(void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    [webData appendData:data];
}
-(void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    NSLog(@"ERROR with theConenction");
    [connection release];
    [webData release];
}
-(void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    NSString *theXML = [[NSString alloc] initWithBytes: [webData mutableBytes] length:[webData length] encoding:NSUTF8StringEncoding];
    NSDictionary *responseXML=[[NSDictionary alloc]initWithDictionary:[XMLReader dictionaryForXMLString:theXML error:nil]];
    [self runCompletionHandler:connection responseXML:responseXML];
    [connection release];
    stopActivityIndicator;
}

-(void)addCompletionHandler: (NSURLConnection*)connection handler:(void (^)(NSDictionary* responseXML))handler{
    CompletionHandler *completionHandler = [[[CompletionHandler alloc]init]autorelease];
    completionHandler.connection = connection;
    completionHandler.handler = handler;
    [_completionHandlers addObject: completionHandler];
}

-(void)runCompletionHandler: (NSURLConnection*)connection responseXML:(NSDictionary*)responseXML{
    for(CompletionHandler *completionHandler in _completionHandlers){
        if(completionHandler.connection == connection){
            if(completionHandler.handler != nil)
                completionHandler.handler(responseXML);
            [_completionHandlers removeObject:completionHandler];
            return;
        }
    }
}

-(NSDictionary*)createRequest:(NSMutableURLRequest*)request completion:(void (^)(NSDictionary* responseXML))completionHandler async:(BOOL)async {
    NSDictionary *returnData;
    if(async){
        NSURLConnection *connection = [[NSURLConnection alloc]initWithRequest:request delegate:self];
        if (connection) {
            [self addCompletionHandler:connection handler:completionHandler];
            startActivityIndicator;
        }
        else {
            NSLog(@"No Connection established");
        }
    }
    else{
        NSURLResponse* response;
        NSError* error = nil;
        
        startActivityIndicator;
        NSData* result =
            [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&error];
        stopActivityIndicator;
        
        NSString *theXML = [[NSString alloc] initWithBytes: [result bytes] length:[result length] encoding:NSUTF8StringEncoding];
        returnData=[[NSDictionary alloc]initWithDictionary:[XMLReader dictionaryForXMLString:theXML error:nil]];
    }
    return returnData;
}

-(NSMutableURLRequest*)getRequestReceiveData:(NSString*)subscriberId data:(NSString *)data{
    NSString *soapFormat = [NSString stringWithFormat:@"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            "<soap:Body>\n"
                            "<ReceiveData xmlns=\"http://sqlite-sync.com/\">\n"
                            "<subscriber>%@</subscriber>\n"
                            "<data>%@</data>"
                            "</ReceiveData>\n"
                            "</soap:Body>\n"
                            "</soap:Envelope>",subscriberId, [self xmlSimpleEscape:data]];
    
    NSURL *locationOfWebService = [NSURL URLWithString:_serverUrl];
    NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc]initWithURL:locationOfWebService];
    NSString *msgLength = [NSString stringWithFormat:@"%lu",(unsigned long)[soapFormat length]];
    [theRequest addValue:@"text/xml" forHTTPHeaderField:@"Content-Type"];
    [theRequest addValue:@"http://sqlite-sync.com/ReceiveData" forHTTPHeaderField:@"SOAPAction"];
    [theRequest addValue:msgLength forHTTPHeaderField:@"Content-Length"];
    [theRequest setHTTPMethod:@"POST"];
    [theRequest setHTTPBody:[soapFormat dataUsingEncoding:NSUTF8StringEncoding]];
    return theRequest;
}
-(NSMutableURLRequest*)getRequestGetDataForSync:(NSString*)subscriberId tableName:(NSString *)tableName{
    NSString *soapFormat = [NSString stringWithFormat:@"<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                            "<soap:Body>"
                            "<GetDataForSync xmlns=\"http://sqlite-sync.com/\">"
                            "<subscriber>\"%@\"</subscriber>"
                            "<table>\"%@\"</table>"
                            "</GetDataForSync>"
                            "</soap:Body>"
                            "</soap:Envelope>",subscriberId, [self xmlSimpleEscape:tableName]];
    
    NSURL *locationOfWebService = [NSURL URLWithString:_serverUrl];
    NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc]initWithURL:locationOfWebService];
    NSString *msgLength = [NSString stringWithFormat:@"%lu",(unsigned long)[soapFormat length]];
    [theRequest addValue:@"text/xml" forHTTPHeaderField:@"Content-Type"];
    [theRequest addValue:@"http://sqlite-sync.com/GetDataForSync" forHTTPHeaderField:@"SOAPAction"];
    [theRequest addValue:msgLength forHTTPHeaderField:@"Content-Length"];
    [theRequest setHTTPMethod:@"POST"];
    [theRequest setHTTPBody:[soapFormat dataUsingEncoding:NSUTF8StringEncoding]];
    return theRequest;
}
-(NSMutableURLRequest*)getRequestSyncCompleted:(long)syncId{
    NSString *soapFormat = [NSString stringWithFormat:@"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            "<soap:Body>\n"
                            "<SyncCompleted xmlns=\"http://sqlite-sync.com/\">\n"
                            "<syncId>%ld</syncId>"
                            "</SyncCompleted>\n"
                            "</soap:Body>\n"
                            "</soap:Envelope>", syncId];
    
    NSURL *locationOfWebService = [NSURL URLWithString:_serverUrl];
    NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc]initWithURL:locationOfWebService];
    NSString *msgLength = [NSString stringWithFormat:@"%lu",(unsigned long)[soapFormat length]];
    [theRequest addValue:@"text/xml" forHTTPHeaderField:@"Content-Type"];
    [theRequest addValue:@"http://sqlite-sync.com/SyncCompleted" forHTTPHeaderField:@"SOAPAction"];
    [theRequest addValue:msgLength forHTTPHeaderField:@"Content-Length"];
    [theRequest setHTTPMethod:@"POST"];
    [theRequest setHTTPBody:[soapFormat dataUsingEncoding:NSUTF8StringEncoding]];
    return theRequest;
}
-(NSMutableURLRequest*)getRequestGetFullDBSchema:(NSString*)subscriberId {
    NSString *soapFormat = [NSString stringWithFormat:@"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            "<soap:Body>\n"
                            "<GetFullDBSchema xmlns=\"http://sqlite-sync.com/\">\n"
                            "<subscriber>\"%@\"</subscriber>\n"
                            "</GetFullDBSchema>\n"
                            "</soap:Body>\n"
                            "</soap:Envelope>",subscriberId];
    
    NSURL *locationOfWebService = [NSURL URLWithString:_serverUrl];
    NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc]initWithURL:locationOfWebService];
    NSString *msgLength = [NSString stringWithFormat:@"%lu",(unsigned long)[soapFormat length]];
    [theRequest addValue:@"text/xml" forHTTPHeaderField:@"Content-Type"];
    [theRequest addValue:@"http://sqlite-sync.com/GetFullDBSchema" forHTTPHeaderField:@"SOAPAction"];
    [theRequest addValue:msgLength forHTTPHeaderField:@"Content-Length"];
    [theRequest setHTTPMethod:@"POST"];
    [theRequest setHTTPBody:[soapFormat dataUsingEncoding:NSUTF8StringEncoding]];
    return theRequest;
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

@implementation CompletionHandler
@end

