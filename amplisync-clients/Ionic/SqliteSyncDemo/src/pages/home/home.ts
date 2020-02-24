import { Component } from '@angular/core';
import { NavController, AlertController } from 'ionic-angular';
import { SqliteSyncProvider } from '../../providers/sqlite-sync/sqlite-sync';
import { ActionSheetController } from 'ionic-angular';
import { TablePage } from '../table/table';

import { Platform } from 'ionic-angular';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  public syncUrl = "https://one-million-demo.ampliapps.com/sync/API3";
  public subscriberId = 1;
  public loadingText = "";
  public isLoading = false;
  public tables = [];

  constructor(public navCtrl: NavController,platform: Platform,
              public sqliteSync: SqliteSyncProvider,
              public actionSheetCtrl: ActionSheetController,
              public alertCtrl: AlertController)
  {
    platform.ready().then(() => {
      this.isLoading = true;
      this.loadingText = "Connecting to database...";
      this.sqliteSync.getTables().then((res) => {
        let data = JSON.parse(JSON.stringify(res));
        this.tables = data;
        this.isLoading = false;
      }).catch((err) => {
        this.isLoading = false;
        let alert = this.alertCtrl.create({
          title: "Error",
          subTitle: err,
          buttons: ["OK"]
        });
        alert.present();
      });
    });
  }

  reinitializeDB(){
    this.isLoading = true;
    this.loadingText = "Reinitializing...";
    this.sqliteSync.InitializeSubscriber(this.syncUrl,this.subscriberId)
    .then((res) => {
      let data = JSON.parse(JSON.stringify(res));
      this.tables = data;
      this.isLoading = false;
    }).catch((err) => {
        this.isLoading = false;
        let alert = this.alertCtrl.create({
          title: "Error",
          subTitle: err,
          buttons: ["OK"]
        });
        alert.present();
    });
  }

  synchronize(){
    this.isLoading = true;
    this.loadingText = "Synchronizing...";
    this.sqliteSync.SynchronizeSubscriber(this.syncUrl, this.subscriberId)
    .then((res) => {
      this.isLoading = false;
    }).catch((err) => {
      this.isLoading = false;
      let alert = this.alertCtrl.create({
        title: "Error",
        subTitle: err,
        buttons: ["OK"]
      });
      alert.present();
    })
  }

  presentActionSheet(){
    let buttons_array = [];
    let self = this;
    this.tables.forEach(function(tblName){
      buttons_array.push({
        text: tblName,
        handler: () => {
          self.show(tblName);
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
      buttons: buttons_array
    });
    actionSheet.present();
  }

  show(tblName){
    this.navCtrl.push(TablePage, {
      'tblName': tblName
    });
  }

}
