import { Component } from '@angular/core';
import { Platform } from 'ionic-angular';
import { AlertController, LoadingController } from 'ionic-angular';
import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';

import { SQLite } from '@ionic-native/sqlite';
import { HomePage } from '../pages/home/home';
import { SqlitesyncServiceProvider } from '../providers/sqlitesync-service/sqlitesync-service';

@Component({
  templateUrl: 'app.html'
})
export class MyApp {
  rootPage:any = HomePage;

  constructor(platform: Platform,
    statusBar: StatusBar,
    splashScreen: SplashScreen,
    public alertCtrl: AlertController,
    public loadingCtrl: LoadingController,
    public sqlitesync: SqlitesyncServiceProvider)
  {
    platform.ready().then(() => {
      statusBar.styleDefault();
      splashScreen.hide();
      this.openDatabase('sqlitesynccom_demo');
    });
  }

  openDatabase(name){
    let loading = this.loadingCtrl.create({
      content: 'Opening database. Please wait...'
    });
    loading.present();
    let db = new SQLite();
    this.sqlitesync.sqlitesync_DB = db.create({ name: name, location: 'default' }).then(() => {
      loading.dismiss();
    }, (error) => {
      loading.dismiss();
      let alert = this.alertCtrl.create({
        title: 'Error',
        message: 'Error while opening database: ' + error,
        buttons: ['Close']
      });
      alert.present();
    });
  }


}
