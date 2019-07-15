/*Screen to view all the user*/
import React from 'react';
import { FlatList, Text, View } from 'react-native';
import { openDatabase } from 'react-native-sqlite-storage';

var db = openDatabase({ name: 'amplisync.db' });
 
export default class ViewAllUser extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      FlatListItems: [],
    };
    db.transaction(tx => {
      tx.executeSql('SELECT * FROM Users', [], (tx, results) => {
        var temp = [];
        for (let i = 0; i < results.rows.length; ++i) {
          temp.push(results.rows.item(i));
          console.log(results.rows.item(i));
        }
        this.setState({
          FlatListItems: temp,
        });
      });
    });
  }
  ListViewItemSeparator = () => {
    return (
      <View style={{ height: 0.2, width: '100%', backgroundColor: '#808080' }} />
    );
  };
  render() {
    return (
      <View>
        <FlatList
          data={this.state.FlatListItems}
          ItemSeparatorComponent={this.ListViewItemSeparator}
          keyExtractor={(item, index) => index.toString()}
          renderItem={({ item }) => (
            <View key={item.usrId} style={{ backgroundColor: 'white', padding: 20 }}>
              <Text>Id: {item.usrId}</Text>
              <Text>Name: {item.usrName}</Text>
              <Text>Last Name: {item.usrLastName}</Text>
              <Text>Age: {item.usrAge}</Text>
              <Text>Login: {item.usrLogin}</Text>
              <Text>Password: {item.usrPass}</Text>
            </View>
          )}
        />
      </View>
    );
  }
}