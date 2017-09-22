import { BrowserModule } from '@angular/platform-browser';
import { ErrorHandler, NgModule } from '@angular/core';
import { IonicApp, IonicErrorHandler, IonicModule } from 'ionic-angular';
import { SplashScreen } from '@ionic-native/splash-screen';
import { StatusBar } from '@ionic-native/status-bar';

import { MyApp } from './app.component';
import { HomePage } from '../pages/home/home';
import { TablePage } from '../pages/table/table';
import { InsertPage } from '../pages/insert/insert';
import { UpdatePage } from '../pages/update/update';
import { SqliteSyncProvider } from '../providers/sqlite-sync/sqlite-sync';
import { SQLite } from '@ionic-native/sqlite';
import { HttpModule } from '@angular/http';

@NgModule({
  declarations: [
    MyApp,
    HomePage,
    TablePage,
    InsertPage,
    UpdatePage
  ],
  imports: [
    BrowserModule,
    IonicModule.forRoot(MyApp),
    HttpModule
  ],
  bootstrap: [IonicApp],
  entryComponents: [
    MyApp,
    HomePage,
    TablePage,
    InsertPage,
    UpdatePage
  ],
  providers: [
    StatusBar,
    SplashScreen,
    {provide: ErrorHandler, useClass: IonicErrorHandler},
    SqliteSyncProvider,
    SQLite
  ]
})
export class AppModule {}
