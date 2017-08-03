import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { ActionSheetController } from 'ionic-angular';
import { SqliteServiceProvider } from '../../providers/sqlite-service/sqlite-service';
import { LoadingController, AlertController } from 'ionic-angular';
import { TablePage } from '../table/table';

declare var sqlitesync_ReinitializeDB: any;
declare var sqlitesync_SyncSendAndReceive: any;
declare var sqlitesync_loading: any;

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  public syncUrl = 'http://65710078.ngrok.io/SqliteSync/API3/';
  public subscriberId = 1;

  constructor(public navCtrl: NavController,
    public actionSheetCtrl: ActionSheetController,
    public sqlite: SqliteServiceProvider,
    public loadingCtrl: LoadingController,
    public alertCtrl: AlertController) 
  {
  }

  reinitializeDB(){
    let loading = this.loadingCtrl.create({
      content: 'Reinitializing...'
    });
    loading.present();
    sqlitesync_loading = true;
    sqlitesync_ReinitializeDB(this.syncUrl, this.subscriberId);
    this.sqlite.getTables()
    .then(()=>{
      loading.dismiss();
    })
    .catch((error)=>{
      loading.dismiss();
    });
  }

  synchronize(){
    let loading = this.loadingCtrl.create({
      content: 'Synchronizing...'
    });
    loading.present();
    sqlitesync_SyncSendAndReceive(this.syncUrl, this.subscriberId);
    loading.dismiss();
  }

  presentActionSheet(){

    let buttons_array = [];
    let self = this;
    this.sqlite.sqlitesync_tables.forEach(function(tbl_name){
      buttons_array.push({
        text: tbl_name,
        handler: () => {
          self.show(tbl_name);
        }
      });
    });

    buttons_array.push({
        text: 'Cancel',
        role: 'cancel',
        icon: 'close'
      });

    let actionSheet = this.actionSheetCtrl.create({
      title: "SELECT * FROM...",
      buttons: buttons_array,
    });
    actionSheet.present();
  }

  show(tbl_name){
    this.navCtrl.push(TablePage, {
      'tbl_name':tbl_name
    });
  }

  isLoading(){
    return sqlitesync_loading;
  }

}
