package com.ampliapps.amplisync;

import com.ampliapps.amplisync.SyncServer.Synchronization.DeviceDataObject;
import com.ampliapps.amplisync.SyncServer.CommonTools;
import com.ampliapps.amplisync.SyncServer.SchemaPublish.SchemaGenerator;
import com.ampliapps.amplisync.SyncServer.Synchronization.SyncService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*************************************************************************
 *
 * CONFIDENTIAL
 * __________________
 *
 *  AMPLIFIER sp. z o.o.
 *  www.ampliapps.com
 *  support@ampliapps.com
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AMPLIFIER sp. z o.o. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to AMPLIFIER sp. z o.o.
 * and its suppliers and may be covered by U.S., European and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AMPLIFIER sp. z o.o..
 **************************************************************************/

@Path("/API3")
public class SyncAPI3 {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String Test(){

        CommonTools commonTools = new CommonTools();
        Boolean isConnectionOK = commonTools.CheckIfDBConnectionIsOK();

        String connectionStatus = "Database connected!";
        if(!isConnectionOK)
            connectionStatus = "Error creating database connection.";

        return "API[" + commonTools.GetVersionOfSQLiteSyncCOM() + "] SQLite-Sync.COM is working correctly! " + connectionStatus;
    }

    @GET
    @Path("/InitializeSubscriber/{subscriberUUID}")
    @Produces(MediaType.TEXT_PLAIN)
    public String InitializeSubscriber(@PathParam("subscriberUUID") String subscriberUUID){
        if(subscriberUUID.isEmpty() || subscriberUUID == null)
            return "";
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.GetFullSchematScript(subscriberUUID);
    }

    @GET
    @Path("/Sync/{subscriberUUID}/{tableId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String SyncChanges(@PathParam("subscriberUUID") String subscriberUUID, @PathParam("tableId") String tableId){
        if(subscriberUUID.isEmpty() || subscriberUUID == null || tableId.isEmpty() || tableId == null)
            return "";
        SyncService sync = new SyncService();
        return sync.DoSync(subscriberUUID, tableId);
    }

    @GET
    @Path("/CommitSync/{syncId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response SyncGetChanges(@PathParam("syncId") String syncId){

        Response response = Response.ok().build();
        if(syncId.isEmpty() || syncId == null)
            return response;
        SyncService sync = new SyncService();
        sync.CommitSync(syncId);

        return response;
    }

    @POST
    @Path("/Send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public void RecieveChanges(DeviceDataObject recievedData){
        SyncService sync = new SyncService();
        sync.ReceiveData(recievedData);
    }

    @GET
    @Path("/AddTable/{tableName}")
    @Produces(MediaType.TEXT_PLAIN)
    public String AddTable(@PathParam("tableName") String tableName){
        if(tableName.isEmpty())
            return "";

        CommonTools commonTools = new CommonTools();
        commonTools.AddTableToSynchronization(tableName);

        return "Table " + tableName + " added to synchronization. Remember to reinitialize clients.";
    }

    @GET
    @Path("/RemoveTable/{tableName}")
    @Produces(MediaType.TEXT_PLAIN)
    public String RemoveTable(@PathParam("tableName") String tableName){
        if(tableName.isEmpty())
            return "";

        CommonTools commonTools = new CommonTools();
        commonTools.RemoveTableFromSynchronization(tableName);

        return "Table " + tableName + " removed from synchronization. Remember to reinitialize clients.";
    }

}
