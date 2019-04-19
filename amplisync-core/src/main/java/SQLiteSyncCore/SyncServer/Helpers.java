package SQLiteSyncCore.SyncServer;

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

public class Helpers {

    public static Boolean TypeConvertionTableIsBLOBType(String SQLType)
    {
        Boolean isBlobType = false;
        switch (SQLType.toLowerCase())
        {
            case "blob":
            case "longblob":
            case "varbinary":
            case "binary":
            case "image":
            case "mediumblob":
            case "varbinarymax":
            case "byte[]":
                isBlobType = true;
                break;
        }

        return isBlobType;
    }

    public static String TypeConvertionTable(String SQLType)
    {
        String SQLiteType = "";
        switch (SQLType)
        {
            case "blob":
            case "longblob":
            case "varbinary":
            case "binary":
            case "image":
            case "mediumblob":
            case "varbinarymax":
            case "byte[]":
                SQLiteType = "BLOB";
                break;
            case "longtext":
            case "varchar":
            case "varchar2":
            case "nvarchar":
            case "nvarchar2":
            case "char":
            case "varcharmax":
            case "enum":
            case "mediumtext":
            case "text":
            case "string":
            case "geography":
            case "geometry":
            case "hierarchyid":
            case "nchar":
            case "ntext":
            case "nvarcharmax":
            case "userdefineddatatype":
            case "userdefinedtabletype":
            case "userdefinedtype":
            case "variant":
            case "xml":
            case "tinytext":
            case "set":
            case "year":
            case "time":
            case "uniqueidentifier":
            case "datetimeoffset":
                SQLiteType = "TEXT";
                break;
            case "timestamp":
            case "datetime2":
            case "datetime":
                SQLiteType = "DATETIME";
                break;
            case "date":
                SQLiteType = "DATE";
                break;
            case "mediumint":
            case "bit":
            case "tinyint":
            case "smallint":
            case "bigint":
            case "int":
            case "boolean":
            case "byte":
            case "long":
            case "int64":
            case "serial":
            case "int32":
            case "smalldatetime":
                SQLiteType = "INTEGER";
                break;
            case "double":
            case "float":
            case "numeric":
            case "decimal":
            case "real":
            case "money":
            case "smallmoney":
                SQLiteType = "REAL";
                break;
            default:
                SQLiteType = "TEXT";
                break;
        }

        return SQLiteType;
    }

    public static String GetSqlDbType(String SQLType)
    {
        String SqlDbType = "";
        switch (SQLType)
        {
            case "blob":
            case "longblob":
            case "mediumblob":
            case "varbinary":
            case "binary":
            case "varbinarymax":
            case "image":
            case "byte[]":
                SqlDbType = "Binary";
                break;
            case "longtext":
            case "varchar":
            case "varchar2":
            case "varcharmax":
            case "nvarchar":
            case "nvarchar2":
            case "enum":
            case "mediumtext":
            case "text":
            case "char":
            case "string":
            case "geography":
            case "geometry":
            case "hierarchyid":
            case "nchar":
            case "ntext":
            case "nvarcharmax":
            case "userdefineddatatype":
            case "userdefinedtabletype":
            case "userdefinedtype":
            case "variant":
            case "xml":
            case "tinytext":
            case "datetimeoffset":
                SqlDbType = "String";
                break;
            case "bit":
            case "boolean":
            case "byte":
                SqlDbType = "Byte";
                break;
            case "tinyint":
                SqlDbType = "Byte";
                break;
            case "smallint":
            case "year":
                SqlDbType = "Int16";
                break;
            case "bigint":
            case "long":
            case "int64":
            case "serial":
                SqlDbType = "Int64";
                break;
            case "mediumint":
            case "int":
            case "int32":
            case "smalldatetime":
                SqlDbType = "Int32";
                break;
            case "double":
                SqlDbType = "Double";
                break;
            case "float":
            case "numeric":
            case "decimal":
            case "money":
            case "real":
            case "smallmoney":
                SqlDbType = "Decimal";
                break;
            case "time":
                SqlDbType = "Time";
                break;
            case "timestamp":
            case "datetime":
            case "datetime2":
            case "date":
                SqlDbType = "DateTime";
                break;
            case "uniqueidentifier":
                SqlDbType = "Guid";
                break;
            default:
                SqlDbType = "String";
                break;
        }

        return SqlDbType;
    }

    public static Boolean IsNullOrBlank(String s)
    {
        return (s==null || s.trim().equals(""));
    }
}
