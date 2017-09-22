import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams, AlertController } from 'ionic-angular';
import { SqliteSyncProvider } from '../../providers/sqlite-sync/sqlite-sync';
import { InsertPage } from '../insert/insert';
import { UpdatePage } from '../update/update';
import { Platform } from 'ionic-angular';
/**
 * Generated class for the TablePage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@IonicPage()
@Component({
  selector: 'page-table',
  templateUrl: 'table.html',
})
export class TablePage {

  public rows;
  public columns;
  public tblName;
  public isLoading = false;
  public loadingText = "";


  constructor(public navCtrl: NavController, platform: Platform,
              public navParams: NavParams,
              public sqliteSync: SqliteSyncProvider,
              public alertCtrl: AlertController)
  {
    platform.ready().then(() => {
      this.rows = [];
      this.columns = [];
      this.tblName = this.navParams.get('tblName');
    });
  }

  ionViewWillEnter(){
    this.getData();
  }

  getData(){
    this.isLoading = true;
    this.loadingText = "Getting data from table...";
    this.rows = [];
    this.columns = [];
    this.sqliteSync.getDataFromTable(this.tblName).then((data) => {
      this.rows = data;
      for(let key in data[0])
        if(key.toLowerCase() !== "MergeUpdate".toLowerCase() && key.toLowerCase() !== "RowId".toLowerCase())
          this.columns.push({
            'name': key,
            'type': typeof((data[0])[key])
          });
      this.isLoading = false;
    }).catch((err) => {
      this.isLoading = false;
      console.log(err);
    });
  }

  goBack(){
    this.navCtrl.pop();
  }

  insertRecord(){
    this.navCtrl.push(InsertPage, {
      tblName: this.tblName,
      columns: this.columns
    });
  }

  deleteRecord(object){
    this.sqliteSync.deleteFromTable(this.tblName, object).then(() => {
      let alert = this.alertCtrl.create({
        title: "Success",
        subTitle: "Record has been deleted.",
        buttons: ["OK"]
      });
      alert.present();
      this.getData();
    }).catch((err) => {
      let alert = this.alertCtrl.create({
        title: "Error",
        subTitle: err,
        buttons: ["OK"]
      });
      alert.present();
    });
  }

  editRecord(object){
    this.navCtrl.push(UpdatePage, {
      tblName: this.tblName,
      row: object
    });
  }

}
