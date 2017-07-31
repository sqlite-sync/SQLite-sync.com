import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { ActionSheetController } from 'ionic-angular';
import { SqlitesyncServiceProvider } from '../../providers/sqlitesync-service/sqlitesync-service';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  public syncUrl = 'http://62f8a76e.ngrok.io/SqliteSync/API3';

  constructor(public navCtrl: NavController, public actionSheetCtrl: ActionSheetController, public sqlitesync: SqlitesyncServiceProvider) {

  }

  reinitializeDB(){
    this.sqlitesync.ReinitializeDB(this.syncUrl, 1);
  }

  presentActionSheet(){
    let actionSheet = this.actionSheetCtrl.create({
      title: "SELECT * FROM...",
      buttons:[
        {
          text:"test1",
          handler: () => {
            console.log("test1 clicked");
          }
        },
        {
          text:"test2",
          handler: () => {
            console.log("test2 clicked");
          }
        },
        {
          text:"test3",
          handler: () => {
            console.log("test3 clicked");
          }
        }
      ]
    });
    actionSheet.present();
  }
}
