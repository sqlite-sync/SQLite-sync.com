import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams } from 'ionic-angular';
import { TablePage } from '../table/table';
import { SqliteServiceProvider } from '../../providers/sqlite-service/sqlite-service';

@IonicPage()
@Component({
  selector: 'page-insert',
  templateUrl: 'insert.html',
})
export class InsertPage {

  public tbl_name;
  public columns;
  public insertModel: any;

  constructor(public navCtrl: NavController, public navParams: NavParams, public sqlite: SqliteServiceProvider) {
    this.insertModel = {};
    this.tbl_name = this.navParams.get('tbl_name');
    let cols = this.navParams.get('columns');
    this.columns = cols.filter(function(c){
      if(c.name != 'RowId' && c.name != 'MergeUpdate') return c;
    })
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad InsertPage');
  }

  backToTable(){
    this.navCtrl.pop();
  }

  submitButton(){
    this.sqlite.insertIntoTable(this.tbl_name, this.insertModel)
    .then(() => {
      this.navCtrl.pop();
    })
    .catch(error => {
      if(error)
        alert(JSON.stringify(error));
    })
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

}
