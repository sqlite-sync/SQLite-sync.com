/*************************************************************************
* 
* SQLite-sync.com CONFIDENTIAL
* __________________
* 
*  SQLite-sync.com Tomasz Dziemidowicz 
*  www.sqlite-sync.com
*  support@sqlite-sync.com
*  All Rights Reserved.
* 
* NOTICE:  All information contained herein is, and remains
* the property of SQLite-sync.com Tomasz Dziemidowicz and its suppliers,
* if any. The intellectual and technical concepts contained
* herein are proprietary to SQLite-sync.com Tomasz Dziemidowicz
* and its suppliers and may be covered by U.S., European and Foreign Patents,
* patents in process, and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from SQLite-sync.com Tomasz Dziemidowicz.
**************************************************************************/
using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Runtime.Serialization;

namespace AuroraSyncService
{
    public class SqliteTable
    {
        private string tableName = string.Empty;
        public string TableName
        {
            get { return tableName; }
            set { tableName = value; }
        }
    }

    public class DeletedRecords
    {
        public string TableId { get; set; }
        public string RowId { get; set; }
    }

    public class DataObject
    {
        private string tableName = string.Empty;
        private string records = string.Empty;
        private string queryInsert = string.Empty;
        private string queryUpdate = string.Empty;
        private string queryDelete = string.Empty;

        private string triggerInsert = string.Empty;
        private string triggerUpdate = string.Empty;
        private string triggerDelete = string.Empty;
        private string triggerInsertDrop = string.Empty;
        private string triggerUpdateDrop = string.Empty;
        private string triggerDeleteDrop = string.Empty;

        private int syncId = 0;

        public string TriggerInsert
        {
            get { return triggerInsert; }
            set { triggerInsert = value; }
        }

        public string TriggerUpdate
        {
            get { return triggerUpdate; }
            set { triggerUpdate = value; }
        }

        public string TriggerDelete
        {
            get { return triggerDelete; }
            set { triggerDelete = value; }
        }

        public string TriggerInsertDrop
        {
            get { return triggerInsertDrop; }
            set { triggerInsertDrop = value; }
        }

        public string TriggerUpdateDrop
        {
            get { return triggerUpdateDrop; }
            set { triggerUpdateDrop = value; }
        }

        public string TriggerDeleteDrop
        {
            get { return triggerDeleteDrop; }
            set { triggerDeleteDrop = value; }
        }

        public int SyncId
        {
            get { return syncId; }
            set { syncId = value; }
        }

        public string QueryUpdate
        {
            get { return queryUpdate; }
            set { queryUpdate = value; }
        }

        public string QueryDelete
        {
            get { return queryDelete; }
            set { queryDelete = value; }
        }

        public string QueryInsert
        {
            get { return queryInsert; }
            set { queryInsert = value; }
        }

        public string TableName
        {
            get { return tableName; }
            set { tableName = value; }
        }

        public string Records
        {
            get { return records; }
            set { records = value; }
        }
    }

    public class DynamicEntity 
    {
        /*
         *TODO: generate dynamic entity 
         */
        public string col0 { get; set; }
        public string col1 { get; set; }
        public string col2 { get; set; }
        public string col3 { get; set; }
        public string col4 { get; set; }
        public string col5 { get; set; }
        public string col6 { get; set; }
        public string col7 { get; set; }
        public string col8 { get; set; }
        public string col9 { get; set; }
        public string col10 { get; set; }
        public string col11 { get; set; }
        public string col12 { get; set; }
        public string col13 { get; set; }
        public string col14 { get; set; }
        public string col15 { get; set; }
        public string col16 { get; set; }
        public string col17 { get; set; }
        public string col18 { get; set; }
        public string col19 { get; set; }
       public string col20 { get; set; }
        public string col21 { get; set; }
        public string col22 { get; set; }
        public string col23 { get; set; }
        public string col24 { get; set; }
        public string col25 { get; set; }
        public string col26 { get; set; }
        public string col27 { get; set; }
        public string col28 { get; set; }
        public string col29 { get; set; }
        public string col30 { get; set; }
        public string col31 { get; set; }
        public string col32 { get; set; }
        public string col33 { get; set; }
        public string col34 { get; set; }
        public string col35 { get; set; }
        public string col36 { get; set; }
        public string col37 { get; set; }
        public string col38 { get; set; }
        public string col39 { get; set; }
        public string col40 { get; set; }
        public string col41 { get; set; }
        public string col42 { get; set; }
        public string col43 { get; set; }
        public string col44 { get; set; }
        public string col45 { get; set; }
        public string col46 { get; set; }
        public string col47 { get; set; }
        public string col48 { get; set; }
        public string col49 { get; set; }
        public string col50 { get; set; }
        public string col51 { get; set; }
        public string col52 { get; set; }
        public string col53 { get; set; }
        public string col54 { get; set; }
        public string col55 { get; set; }
        public string col56 { get; set; }
        public string col57 { get; set; }
        public string col58 { get; set; }
        public string col59 { get; set; }
        public string col60 { get; set; }
        public string col61 { get; set; }
        public string col62 { get; set; }
        public string col63 { get; set; }
        public string col64 { get; set; }
        public string col65 { get; set; }
        public string col66 { get; set; }
        public string col67 { get; set; }
        public string col68 { get; set; }
        public string col69 { get; set; }
        public string col70 { get; set; }
        public string col71 { get; set; }
        public string col72 { get; set; }
        public string col73 { get; set; }
        public string col74 { get; set; }
        public string col75 { get; set; }
        public string col76 { get; set; }
        public string col77 { get; set; }
        public string col78 { get; set; }
        public string col79 { get; set; }
        public string col80 { get; set; }
        public string col81 { get; set; }
        public string col82 { get; set; }
        public string col83 { get; set; }
        public string col84 { get; set; }
        public string col85 { get; set; }
        public string col86 { get; set; }
        public string col87 { get; set; }
        public string col88 { get; set; }
        public string col89 { get; set; }
        public string col90 { get; set; }
        public string col91 { get; set; }
        public string col92 { get; set; }
        public string col93 { get; set; }
        public string col94 { get; set; }
        public string col95 { get; set; }
        public string col96 { get; set; }
        public string col97 { get; set; }
        public string col99 { get; set; }
        public string col100 { get; set; }
    }
}