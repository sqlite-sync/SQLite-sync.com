import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { ActionSheetController } from 'ionic-angular';
import { SqliteServiceProvider } from '../../providers/sqlite-service/sqlite-service';
import { LoadingController, AlertController } from 'ionic-angular';
import { TablePage } from '../table/table';

declare var sqlitesync_ReinitializeDB: any;
declare var sqlitesync_SyncSendAndReceive: any;
declare var sqlitesync_loading: boolean;

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {
  public syncUrl = 'http://18aa1aea.ngrok.io/SqliteSync/API3/';
  public subscriberId = 1;
  public loadingText = 'Reinitializing...';
  public isLoading = function(){return sqlitesync_loading;};

  constructor(public navCtrl: NavController,
    public actionSheetCtrl: ActionSheetController,
    public sqlite: SqliteServiceProvider,
    public loadingCtrl: LoadingController,
    public alertCtrl: AlertController) 
  {
  }

  reinitializeDB(){
    this.loadingText = 'Reinitializing...';
    sqlitesync_ReinitializeDB(this.syncUrl, this.subscriberId);
    this.sqlite.getTables();
  
  }

  synchronize(){
    this.loadingText = 'Synchronizing...';
    sqlitesync_SyncSendAndReceive(this.syncUrl, this.subscriberId);
  }

  presentActionSheet(){

    let buttons_array = [];
    let self = this;
    this.sqlite.sqlitesync_tables.forEach(function(tbl_name){
      buttons_array.push({        text: tbl_name,
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

}
