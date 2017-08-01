import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams } from 'ionic-angular';
import { InsertPage } from '../insert/insert';
import { HomePage } from '../home/home';

/**
 * Generated class for the TablePage page.
 *
 * See http://ionicframework.com/docs/components/#navigation for more info
 * on Ionic pages and navigation.
 */

@IonicPage()
@Component({
  selector: 'page-table',
  templateUrl: 'table.html',
})
export class TablePage {

  public tbl_name;

  constructor(public navCtrl: NavController, public navParams: NavParams) {
    this.tbl_name = this.navParams.get('tbl_name');
  }

  goBack(){
    this.navCtrl.push(HomePage);
  }

  addPage(){
    this.navCtrl.push(InsertPage);
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad TablePage');
  }
}
