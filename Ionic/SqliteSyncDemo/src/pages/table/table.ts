import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams } from 'ionic-angular';
import { SqliteServiceProvider } from '../../providers/sqlite-service/sqlite-service';
import { LoadingController } from 'ionic-angular';
import { InsertPage } from '../insert/insert';
import { HomePage } from '../home/home';


@IonicPage()
@Component({
  selector: 'page-table',
  templateUrl: 'table.html',
})
export class TablePage {

  public tbl_name;
  public data;

  constructor(public navCtrl: NavController, public navParams: NavParams, public sqlite: SqliteServiceProvider, public loadingCtrl: LoadingController) {
    let loading = this.loadingCtrl.create({
      content: 'Loading data...'
    });
    loading.present();
    this.tbl_name = this.navParams.get('tbl_name');
    this.sqlite.getDataFromTable(this.tbl_name)
    .then((data) => {
      this.data = data;
      loading.dismiss();
    })
    .catch((error) => {
      loading.dismiss();
      alert('Error - ' + error);
      this.navCtrl.pop();
    });
  }

  goBack(){
    this.navCtrl.pop();
  }

  addPage(){
    this.navCtrl.push(InsertPage);
  }

  ionViewDidLoad() {
  }

}
