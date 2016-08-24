using SQLite.Net;
using SQLiteSyncCOMCsharp;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Popups;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace SQLiteSyncCOM_UWP
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public MainPage()
        {
            this.InitializeComponent();            
        }

        private async void button_Click(object sender, RoutedEventArgs e)
        {
            var dbPath = System.IO.Path.Combine(Windows.Storage.ApplicationData.Current.LocalFolder.Path, "sqlitesynccom.sqlite");
            SQLiteSyncCOMClient sqliteSyncComClient = new SQLiteSyncCOMClient(dbPath, txtServerUrl.Text);
            await sqliteSyncComClient.ReinitializeDatabase(txtSubscriberId.Text);
            var dialog = new MessageDialog("Reinitialization done!");
            await dialog.ShowAsync();
        }

        private async void btnSendAndRecieve_Click(object sender, RoutedEventArgs e)
        {
            var dbPath = System.IO.Path.Combine(Windows.Storage.ApplicationData.Current.LocalFolder.Path, "sqlitesynccom.sqlite");
            SQLiteSyncCOMClient sqliteSyncComClient = new SQLiteSyncCOMClient(dbPath, txtServerUrl.Text);
            await sqliteSyncComClient.SendAndRecieveChanges(txtSubscriberId.Text);
            var dialog = new MessageDialog("Synchronization complete!");
            await dialog.ShowAsync();
        }

        private async void LoadSampleData()
        {
            try
            {
                var dbPath = System.IO.Path.Combine(Windows.Storage.ApplicationData.Current.LocalFolder.Path, "sqlitesynccom.sqlite");
                using (SQLiteConnection db = new SQLiteConnection(new SQLite.Net.Platform.WinRT.SQLitePlatformWinRT(), dbPath))
                {
                    var entities = db.Query<Entities>("select entName,entAdress from Entities;");
                    gridView.ItemsSource = entities;

                }
            }
            catch (Exception)
            {
                var dialog = new MessageDialog("Reinitialize and synchronize database first!");
                await dialog.ShowAsync();
            }
        }

        private void Grid_Loaded(object sender, RoutedEventArgs e)
        {
            LoadSampleData();
        }
    }
}
