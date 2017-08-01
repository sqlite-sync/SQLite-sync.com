import { Component } from '@angular/core';
import { Platform } from 'ionic-angular';
import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';

import { HomePage } from '../pages/home/home';
import { SqliteServiceProvider } from '../providers/sqlite-service/sqlite-service';

@Component({
  templateUrl: 'app.html'
})
export class MyApp {
  rootPage:any = HomePage;

  constructor(platform: Platform,
    statusBar: StatusBar,
    splashScreen: SplashScreen,
    public sqlite: SqliteServiceProvider)
  {
    platform.ready().then(() => {
      statusBar.styleDefault();
      splashScreen.hide();
      this.sqlite.openDatabase();
    });
  }
}
