Ext.define('SQLiteSyncDEMO.controller.SQLiteSyncDEMOCtrl', {
    extend: 'Ext.app.Controller',

    config: {
    },

    init: function() {
        this.control({
            'button[action=sync]':{
                tap: this.startSync
            },

            'button[action=reinitialize]':{
                tap: this.reinitializeDatabase
            },

            'button[action=editEntityRecord]':{
                tap: this.editEntityRecord
            },

            'button[action=addEntityRecord]':{
                tap: this.addEntityRecord
            },

            'button[action=saveEntityRecord]':{
                tap: this.saveEntityRecord
            },

            'button[action=deleteEntityRecord]':{
                tap: this.deleteEntityRecord
            }
        })
    },

    startSync: function(){
        var syncUrl = '';
        var syncURLCtrl = Tools.getElement('textfield[id="syncURL"]');
        if(syncURLCtrl != undefined && syncURLCtrl != null)
            syncUrl =syncURLCtrl.getValue();
        var syncPdaIdent = '';
        var syncPdaIdentCrtl = Tools.getElement('textfield[id="syncPdaIdent"]');
        if(syncPdaIdentCrtl != undefined && syncPdaIdentCrtl != null)
            syncPdaIdent =syncPdaIdentCrtl.getValue();
        sqlitesync_SyncSendAndReceive(syncUrl,syncPdaIdent);
    },

    reinitializeDatabase: function(){
        var syncUrl = '';
        var syncURLCtrl = Tools.getElement('textfield[id="syncURL"]');
        if(syncURLCtrl != undefined && syncURLCtrl != null)
            syncUrl =syncURLCtrl.getValue();
        var syncPdaIdent = '';
        var syncPdaIdentCrtl = Tools.getElement('textfield[id="syncPdaIdent"]');
        if(syncPdaIdentCrtl != undefined && syncPdaIdentCrtl != null)
            syncPdaIdent =syncPdaIdentCrtl.getValue();
        sqlitesync_ReinitializeDB(syncUrl,syncPdaIdent);
    },

    addEntityRecord: function(){
        this.showEntityPopup(0, '', '');
    },

    editEntityRecord: function(){

        var entId = 0;
        var entName = '';
        var entAddress = '';

        var lstEntities = Tools.getElement('list[id="lstEntities"]');
        if(lstEntities != undefined && lstEntities != null && lstEntities.getSelection().length > 0){
            entId = lstEntities.getSelection()[0].data.entId;
            entName = lstEntities.getSelection()[0].data.entName;
            entAddress = lstEntities.getSelection()[0].data.entAddress;
            this.showEntityPopup(entId, entName, entAddress);
        } else {
            Ext.Msg.alert('SQLite-Sync.com', 'Select row first!' , Ext.emptyFn);
        }
    },

    showEntityPopup:function(_entId, _entName, _entAddress){
        var entId = _entId;
        var entName = _entName;
        var entAddress = _entAddress;
        var popup = Ext.create('Ext.Panel', {
            floating        : true,
            modal           : true,
            centered        : true,
            width           : '90%',
            height          : '90%',
            hideOnMaskTap	: false,
            layout: 'card',
            id: 'entityPopup',
            items: [
                {
                    docked: 'top',
                    xtype : 'toolbar',
                    title : 'Entity',
                    items: [{
                        xtype: 'button',
                        margin: 5,
                        ui: 'decline',
                        align: 'left',
                        text: 'Cancel',
                        handler: function() { popup.hide() }
                    },{xtype:'spacer'},{
                        xtype: 'button',
                        margin: 5,
                        ui: 'confirm',
                        align: 'right',
                        text: 'Save',
                        action: 'saveEntityRecord'
                    }]
                },{
                    xtype: 'fieldset',
                    scrollable: true,
                    items: [{
                        xtype: 'hiddenfield',
                        name: 'entId',
                        value: entId
                    },{
                        xtype: 'textfield',
                        name: 'entName',
                        label: 'Name:',
                        value: entName
                    }, {
                        xtype: 'textfield',
                        name: 'entAddress',
                        label: 'Address:',
                        value: entAddress
                    }] // items
                }
            ],
            scrollable: false
        });

        popup.on('hide', function(me) {
            me.destroy();
        });

        Ext.Viewport.add(popup);
    },

    deleteEntityRecord: function(){
        var lstEntities = Tools.getElement('list[id="lstEntities"]');
        if(lstEntities != undefined && lstEntities != null && lstEntities.getSelection().length > 0){
            var entId = lstEntities.getSelection()[0].data.entId;

            database.Main.dbConn.transaction(function (transaction) {

                    transaction.executeSql("delete from entities where entId=?", [
                        entId
                    ],
                        function (transaction, results){

                        },function(transaction, error){
                            console.log(error);
                        });

            }, function(error){
                console.log(error);
            }, function(){
                Ext.getStore('EntitiesStore').load();
            });

        } else {
            Ext.Msg.alert('SQLite-Sync.com', 'Select row first!' , Ext.emptyFn);
        }
    },

    saveEntityRecord: function(){

        var entId = 0;
        var entName = '';
        var entAddress = '';

        var entIdCtrl = Tools.getElement('hiddenfield[name="entId"]');
        if(entIdCtrl != undefined && entIdCtrl != null)
            entId = entIdCtrl.getValue();

        var entNameCtrl = Tools.getElement('textfield[name="entName"]');
        if(entNameCtrl != undefined && entNameCtrl != null)
            entName = entNameCtrl.getValue();

        var entAddressCtrl = Tools.getElement('textfield[name="entAddress"]');
        if(entAddressCtrl != undefined && entAddressCtrl != null)
            entAddress = entAddressCtrl.getValue();

        database.Main.dbConn.transaction(function (transaction) {
            if(entId == 0){
                transaction.executeSql("insert into entities (entId, entName, entAddress, entEnabled) values (?,?,?,?)", [
                    0,
                    entName,
                    entAddress,
                    1
                ],
                    function (transaction, results){

                    },function(transaction, error){
                        console.log(error);
                    });
            } else {
                transaction.executeSql("update entities set entName=?, entAddress=? where entId=?", [
                    entName,
                    entAddress,
                    entId
                ],
                    function (transaction, results){

                    },function(transaction, error){
                        console.log(error);
                    });
            }
        }, function(error){
            console.log(error);
        }, function(){
            Ext.getStore('EntitiesStore').load();
        });

        var entityPopup = Tools.getElement('panel[id="entityPopup"]');
        if(entityPopup != undefined && entityPopup != null)
            entityPopup.hide();
    }

});