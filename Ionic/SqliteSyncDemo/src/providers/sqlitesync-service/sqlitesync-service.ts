import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { AlertController, LoadingController } from 'ionic-angular';
import { SQLite } from '@ionic-native/sqlite';
import 'rxjs/add/operator/map';

@Injectable()
export class SqlitesyncServiceProvider {

  public sqlitesync_DB: any;

  constructor(public http: Http, public alertCtrl: AlertController, public loadingCtrl: LoadingController) {
  }

  public ReinitializeDB(syncUrl, subscriberId){
    let loading = this.loadingCtrl.create({
      content: 'Reinitalizing...'
    });
    loading.present();
    let URL = syncUrl + '/InitializeSubscriber/' + subscriberId;
    console.log(URL);
    this.http.get(URL)
      .map(res => res.json())
      .subscribe(res => {
        console.log(res);
        loading.dismiss();
        this.sqlitesync_DB.transaction(function(tx){
          Object.keys(res)
            .sort()
            .forEach(function(v,i){
              tx.executeSql(res[v],
              null,
              function (transaction, result) {
              },
              function (transaction, error) {
              });
            });
        }, function (error){
          let alert = this.alertCtrl.create({
            title: 'Error',
            message: 'Error while syncing with the server!',
            buttons: ['Close']
          });
          alert.present();
        }, function (){
          let alert = this.alertCtrl.create({
            title: 'Success',
            message: 'Initialization completed!',
            buttons: ['Close']
          });
          alert.present();
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
