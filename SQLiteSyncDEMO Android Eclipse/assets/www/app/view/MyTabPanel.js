Ext.define('SQLiteSyncDEMO.view.MyTabPanel', {
    extend: 'Ext.tab.Panel',

    config: {
        items: [
            {
                xtype: 'container',
                title: 'Config',
                iconCls: 'settings',
                items: [
                    {
                        xtype: 'fieldset',
                        title: 'SQLiteSync config',
                        items: [
                            {
                                xtype: 'textfield',
                                id:'syncURL',
                                label: 'Sync server URL',
                                value: 'http://demo.sqlite-sync.com/'
                            },
                            {
                                xtype: 'textfield',
                                id: 'syncPdaIdent',
                                label: 'Subscriber ID',
                                value: '1'
                            }
                        ]
                    },
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        items:[
                            {
                                xtype: 'button',
                                style: 'margin: 5px;',
                                action: 'reinitialize',
                                text: 'Reinitialize database'
                            },{
                                xtype: 'button',
                                style: 'margin: 5px;',
                                action: 'sync',
                                text: 'Synchronize'
                            }
                        ]
                    },
                    {
                        xtype: 'container',
                        height: '30px',
                        html: '<b>Log</b>'
                    },
                    {
                        xtype: 'dataview',
                        scroll: 'vertical',
                        height: '300px',
                        id: 'lstLog'
                    },
                    {
                        xtype: 'toolbar',
                        docked: 'top',
                        title: 'SQLite-Sync.com DEMO'
                    }
                ]
            },
            {
                xtype: 'container',
                title: 'Entities',
                iconCls: 'action',
                layout: 'fit',
                items: [
                    {
                        xtype: 'list',
                        id: 'lstEntities',
                        store: 'EntitiesStore',
                        itemTpl: [
                            '<div>{entId},{entName},{entAddress},{entEnabled}</div>'
                        ]
                    },
                    {
                        xtype: 'toolbar',
                        docked: 'top',
                        items: [
                            {
                                xtype: 'button',
                                text: 'Add new record',
                                action: 'addEntityRecord'
                            },
                            {
                                xtype: 'button',
                                text: 'Edit record',
                                action: 'editEntityRecord'
                            },
                            {
                                xtype: 'button',
                                text: 'Delete record',
                                action: 'deleteEntityRecord'
                            }
                        ]
                    }
                ]
            },
            {
                xtype: 'container',
                title: 'Users',
                iconCls: 'action',
                layout: 'fit',
                items: [
                    {
                        xtype: 'list',
                        id: 'lstUsers',
                        store: 'UsersStore',
                        itemTpl: [
                            '<div>{usrId},{usrName},{usrLastName},{usrAge},{usrLogin},{usrPass}</div>'
                        ]
                    },
                    {
                        xtype: 'toolbar',
                        docked: 'top'
                    }
                ]
            },
            {
                xtype: 'container',
                title: 'Documents',
                iconCls: 'action',
                layout: 'fit',
                items: [
                    {
                        xtype: 'list',
                        id: 'lstDocuments',
                        store: 'DocumentsStore',
                        itemTpl: [
                            '<div>{docId},{docName},{docSize},{docDate},{docValue1}</div>'
                        ]
                    },
                    {
                        xtype: 'toolbar',
                        docked: 'top'
                    }
                ]
            }
        ],
        tabBar: {
            docked: 'bottom'
        }
    }

});