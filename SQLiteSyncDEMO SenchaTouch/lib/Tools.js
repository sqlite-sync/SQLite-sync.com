var Tools = {
    showMask:function(){
        Ext.Viewport.setMasked({xtype: 'loadmask', message: 'Proszę czekać...', indicator:true});
    },

    hideMask:function(){
        Ext.Viewport.setMasked(false);
    },

    getElement:function(id){
        var cmp = Ext.ComponentQuery.query(id);
        return cmp[cmp.length - 1];
    }
}