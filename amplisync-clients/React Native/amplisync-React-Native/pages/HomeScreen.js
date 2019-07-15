/*Home Screen With buttons to navigate to diffrent options*/
import React from 'react';
import { View } from 'react-native';
import Mybutton from './components/Mybutton';
import Mytext from './components/Mytext';
import { openDatabase } from 'react-native-sqlite-storage';
var db = openDatabase({ name: 'amplisync.db' });
 
export default class HomeScreen extends React.Component {
  constructor(props) {
    super(props);
  }
 
  render() {
    return (
      <View
        style={{
          flex: 1,
          backgroundColor: 'white',
          flexDirection: 'column',
        }}>
        <Mytext text="AMPLI-SYNC Example" />
        <Mybutton
          title="Synchronize"
          customClick={() => this.props.navigation.navigate('AmpliSync')}
        />
        <Mybutton
          title="Users"
          customClick={() => this.props.navigation.navigate('ViewAllUser')}
        />        
      </View>
    );
  }
}