import { Component } from '@angular/core';
import { Platform } from 'ionic-angular';
import { AlertController, LoadingController } from 'ionic-angular';
import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';

import { SQLite, SQLiteObject } from '@ionic-native/sqlite';
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
      this.openDatabase();
    });
  }

  openDatabase(){
    let name = 'sqlitesynccom_demo';
    let loading = this.loadingCtrl.create({
      content: 'Opening database. Please wait...'
    });
    loading.present();
    let sqlite_db = new SQLite();
    sqlite_db.create({ name: name, location: 'default' }).then((db: SQLiteObject) => {
      this.sqlitesync.sqlitesync_DB = db;
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
