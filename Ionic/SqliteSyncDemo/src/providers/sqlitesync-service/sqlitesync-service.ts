import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { AlertController, LoadingController } from 'ionic-angular';
import { SQLite, SQLiteObject } from '@ionic-native/sqlite';
import 'rxjs/add/operator/map';

@Injectable()
export class SqlitesyncServiceProvider {

  public sqlitesync_DB: SQLiteObject;
  public sqlitesync_tables: any;

  constructor(public http: Http, public alertCtrl: AlertController, public loadingCtrl: LoadingController) {
    Promise.all([
    this.getTables()
    ]).then(val => {
      if(!this.sqlitesync_tables)
        this.sqlitesync_tables = [];
    });
  }

  private getTables(){
    if(this.sqlitesync_DB){
      this.sqlitesync_DB.executeSql("SELECT tbl_name FROM sqlite_master WHERE type='table'", {})
      .then( (data) => {
        this.sqlitesync_tables = [];
        for (let i = 0; i < data.rows.length; i++){
          if(data.rows.item(i).tbl_name !== "android_metadata")
            this.sqlitesync_tables.push(data.rows.item(i).tbl_name);
        }
      })
      .catch( error => {
        this.sqlitesync_tables = [];
      });
    }
  }

<<<<<<< HEAD
=======


>>>>>>> 224b8273d0c5fb06ec7b613a6afe5361943e04e8
  public ReinitializeDB(syncUrl, subscriberId){
    let loading = this.loadingCtrl.create({
      content: 'Reinitializing...'
    });
    loading.present();
    let URL = syncUrl + '/InitializeSubscriber/' + subscriberId;
    console.log(URL);
    this.http.get(URL)
    .map(res => res.json())
    .subscribe(res => {
      let self = this;
      Object.keys(res)
      .sort()
      .forEach(function(v,i){
        self.sqlitesync_DB.executeSql(res[v],{})
        .then( () => {
            //alert('Created object ' + v);
        })
        .catch( e => {
          //alert('Error ' + v + ' - ' + e.message);
        });
      });
      this.getTables();
      loading.dismiss();
    }, err => {
      loading.dismiss();
      let alert = this.alertCtrl.create({
        title: 'Error',
        message: 'Error while connecting to the server!',
        buttons: ['Close']
      });
      alert.present();
    });
  }
}
