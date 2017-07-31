import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { ActionSheetController } from 'ionic-angular';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  constructor(public navCtrl: NavController, public actionSheetCtrl: ActionSheetController) {

  }

  presentActionSheet(){
    let actionSheet = this.actionSheetCtrl.create({
      title: "SELECT * FROM...",
      buttons:[
        {
          text:"test1",
          handler: () => {
            console.log("test1 clicked");
          }
        },
        {
          text:"test2",
          handler: () => {
            console.log("test2 clicked");
          }
        },
        {
          text:"test3",
          handler: () => {
            console.log("test3 clicked");
          }
        }
      ]
    });
    actionSheet.present();
  }
}
