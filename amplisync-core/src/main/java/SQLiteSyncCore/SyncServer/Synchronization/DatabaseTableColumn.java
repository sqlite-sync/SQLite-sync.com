package SQLiteSyncCore.SyncServer.Synchronization;

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

public class DatabaseTableColumn
{
    public String Name =  "";
    public String DefaultValue =  "";
    public Boolean AllowDBNull = false;
    public String DataTypeName = "";
    public String SqlDataTypeName =  "";
    public Boolean IsAutoIncrement = false;
    public Boolean IsInPrimaryKey = false;
}
