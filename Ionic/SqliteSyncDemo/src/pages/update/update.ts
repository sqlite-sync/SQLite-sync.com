import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams, AlertController } from 'ionic-angular';
import { SqliteSyncProvider } from '../../providers/sqlite-sync/sqlite-sync';

/**
 * Generated class for the UpdatePage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@IonicPage()
@Component({
  selector: 'page-update',
  templateUrl: 'update.html',
})
export class UpdatePage {

  public tblName;
  public columns;
  public row;
  public insertModel: any;
  public isLoading = false;
  public loadingText = "";

  constructor(public navCtrl: NavController,
              public navParams: NavParams,
              public sqliteSync: SqliteSyncProvider,
              public alertCtrl: AlertController)
  {
    this.insertModel = {};
    this.columns = [];
    this.tblName = this.navParams.get('tblName');
    this.row = this.navParams.get('row');
    for(let prop in this.row){
      if(prop.toLowerCase() !== "MergeUpdate".toLowerCase() && prop.toLowerCase() !== "RowId".toLowerCase()){
        this.columns.push({
          name: prop,
          inputType: this.getType(typeof(this.row[prop]))
        });
        this.insertModel[prop] = this.row[prop];
      }
    }
    console.log(JSON.stringify(this.columns));
    console.log(JSON.stringify(this.insertModel));

  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad UpdatePage');
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

  backToTable(){
    this.navCtrl.pop();
  }

  updateRecord(){
    this.isLoading = true;
    this.loadingText = "Updating record...";
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
    columnValues.push(this.row["RowId"]);
    this.sqliteSync.updateRecord(this.tblName, columnNames, columnValues).then(() => {
      this.isLoading = false;
      let alert = this.alertCtrl.create({
        title: "Success",
        subTitle: "Row has been updated correctly. You'll be moved to previous page.",
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
