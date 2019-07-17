/*Example of SQLite Database in React Native*/
import React from 'react';
//In Version 2+
//import {createStackNavigator} from 'react-navigation';
//In Version 3+
import {createStackNavigator,createAppContainer} from 'react-navigation';
import HomeScreen from './pages/HomeScreen';
import AmpliSync from './pages/AmpliSync';
import ViewAllUser from './pages/ViewAllUser';
import AddNewUser from './pages/AddNewUser';
 
const App = createStackNavigator({
  HomeScreen: {
    screen: HomeScreen,
    navigationOptions: {
      title: 'HomeScreen',
      headerStyle: { backgroundColor: '#4ecac2' },
      headerTintColor: '#ffffff',
    },
  },
  AmpliSync: {
    screen: AmpliSync,
    navigationOptions: {
      title: 'Synchronization',
      headerStyle: { backgroundColor: '#4ecac2' },
      headerTintColor: '#ffffff',
    },
  },  
  ViewAllUser: {
    screen: ViewAllUser,
    navigationOptions: {
      title: 'Users',
      headerStyle: { backgroundColor: '#4ecac2' },
      headerTintColor: '#ffffff',
    },
  },
  AddNewUser: {
    screen: AddNewUser,
    navigationOptions: {
      title: 'New User',
      headerStyle: { backgroundColor: '#4ecac2' },
      headerTintColor: '#ffffff',
    },
  },      
});
//For React Navigation Version 2+
//export default App;
//For React Navigation Version 3+
export default createAppContainer(App);