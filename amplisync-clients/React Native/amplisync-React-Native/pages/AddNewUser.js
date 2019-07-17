/*Home Screen With buttons to navigate to diffrent options*/
import React from 'react';
import { View, Alert } from 'react-native';
import Mybutton from './components/Mybutton';
import { openDatabase } from 'react-native-sqlite-storage';
import Mytextinput from './components/Mytextinput';
var db = openDatabase({ name: 'amplisync.db' });
 
export default class AddNewUser extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
        firstName: '',
        lastName: '',
        age: '',
        login: '',
        password: '',
      };    
  }
 
  saveUser = () => {
    const { firstName } = this.state;
    const { lastName } = this.state;
    const { age } = this.state;
    const { login } = this.state;
    const { password } = this.state;

    db.transaction(tx => {
        tx.executeSql('insert into Users ([usrName],[usrLastName],[usrAge],[usrLogin],[usrPass]) values (?,?,?,?,?)', [firstName, lastName, age, login, password], (tx, results) => {
            Alert.alert(
                'Success',
                'User added!',
                [
                  {
                    text: 'Ok',
                  },
                ],
                { cancelable: false }
              );
        });
      }, null, () => {
        this.props.navigation.navigate('ViewAllUser');
      });
  }

  render() {
    return (
      <View
        style={{
          flex: 1,
          backgroundColor: 'white',
          flexDirection: 'column',
        }}>        
        <Mytextinput
          placeholder="First Name"
          onChangeText={firstName => this.setState({ firstName })}
          style={{ padding: 10 }}
        />
        <Mytextinput
          placeholder="Last Name"
          onChangeText={lastName => this.setState({ lastName })}
          style={{ padding: 10 }}
        />
        <Mytextinput
          placeholder="Age"
          onChangeText={age => this.setState({ age })}
          style={{ padding: 10 }}
        />
        <Mytextinput
          placeholder="Login"
          onChangeText={login => this.setState({ login })}
          style={{ padding: 10 }}
        />
        <Mytextinput
          placeholder="Password"
          onChangeText={password => this.setState({ password })}
          style={{ padding: 10 }}
        />
        
        <Mybutton
          title="Save"
          customClick={this.saveUser.bind(this)}
        />    
      </View>
    );
  }
}