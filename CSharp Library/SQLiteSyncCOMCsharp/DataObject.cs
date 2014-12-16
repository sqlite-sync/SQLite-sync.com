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
using System.Web;
using System.Runtime.Serialization;

namespace AuroraSyncService
{    
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
}