import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams, AlertController } from 'ionic-angular';
import { SqliteSyncProvider } from '../../providers/sqlite-sync/sqlite-sync';
/**
 * Generated class for the InsertPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@IonicPage()
@Component({
  selector: 'page-insert',
  templateUrl: 'insert.html',
})
export class InsertPage {

  public tbl_name;
  public columns;
  public insertModel: any;
  public isLoading = false;
  public loadingText = "";

  constructor(public navCtrl: NavController,
              public navParams: NavParams,
              public sqliteSync: SqliteSyncProvider,
              public alertCtrl: AlertController)
  {
    this.insertModel = {};
    this.tbl_name = this.navParams.get('tblName');
    this.columns = this.navParams.get('columns');
    for(let column of this.columns){
      column.inputType = this.getType(column.type);
    }
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad InsertPage');
  }

  backToTable(){
    this.navCtrl.pop();
  }

  getType(type){
    switch(type){
      case 'string':
        return 'text';
      case 'number':
        return 'number';
      default:
        return 'text';
    }
  }

  insertRecord(){
    this.isLoading = true;
    this.loadingText = "Inserting into table...";
    let columnNames = [];
    let columnValues = [];
    for(let column of this.columns){
      if(this.insertModel[column.name] == null){
        this.isLoading = false;
        let alert = this.alertCtrl.create({
          title: "Empty fields",
          subTitle: "Please fill all fields correctly.",
          buttons: ["OK"]
        });
        alert.present();
        return;
      }
      columnValues.push(this.insertModel[column.name]);
      columnNames.push(column.name);
    }
    this.sqliteSync.insertIntoTable(this.tbl_name, columnNames, columnValues).then(() => {
      this.isLoading = false;
      let alert = this.alertCtrl.create({
        title: "Success",
        subTitle: "Row has been inserted correctly. You'll be moved to previous page.",
        buttons: [{
          text: "OK",
          handler: () => { this.navCtrl.pop(); }
        }]
      });
      alert.present();
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

}
