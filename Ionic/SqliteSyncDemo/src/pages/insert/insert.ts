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
  public data;

  constructor(public navCtrl: NavController, public navParams: NavParams, public sqlite: SqliteServiceProvider) {

    this.tbl_name = this.navParams.get('tbl_name');
    
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad InsertPage');
  }

  backToTable(){
    this.navCtrl.pop();
  }

  submitButton(){
    
  }
}
