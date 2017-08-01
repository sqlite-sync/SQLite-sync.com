import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { ActionSheetController } from 'ionic-angular';
import { SqlitesyncServiceProvider } from '../../providers/sqlitesync-service/sqlitesync-service';
import { TablePage } from '../table/table';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  public syncUrl = 'http://62f8a76e.ngrok.io/SqliteSync/API3';

  constructor(public navCtrl: NavController, public actionSheetCtrl: ActionSheetController, public sqlitesync: SqlitesyncServiceProvider) {
  }

    reinitializeDB(){
      this.sqlitesync.ReinitializeDB(this.syncUrl, 1)
    }

  presentActionSheet(){
    
    //this.sqlitesync.sqlitesync_tables

    let buttons_array = [];
    let self = this;
    this.sqlitesync.sqlitesync_tables.forEach(function(tbl_name){
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

}
