import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { AlertController, LoadingController } from 'ionic-angular';
import { SQLite, SQLiteObject } from '@ionic-native/sqlite';
import 'rxjs/add/operator/map';

@Injectable()
export class SqlitesyncServiceProvider {

  public sqlitesync_DB: SQLiteObject;

  constructor(public http: Http, public alertCtrl: AlertController, public loadingCtrl: LoadingController) {
  }

  public ReinitializeDB(syncUrl, subscriberId){
    alert(this.sqlitesync_DB);
    let loading = this.loadingCtrl.create({
      content: 'Reinitializing...'
    });
    loading.present();
    let URL = syncUrl + '/InitializeSubscriber/' + subscriberId;
    console.log(URL);
    this.http.get(URL)
    .map(res => res.json())
    .subscribe(res => {
      loading.dismiss();
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
