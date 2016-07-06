using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SQLite;
using System.IO;
using Mono.Data.Sqlite;
using Xamarin.Forms;
//using UIKit;

namespace SQLiteSyncCOMLibXamarin
{
	public class HomePageCS : ContentPage
	{
		ListView lst;

		public HomePageCS ()
		{
			Button synchronize = new Button
			{
				Text = "Synchronize",
				FontAttributes = FontAttributes.Bold,
				HorizontalOptions = LayoutOptions.Center
			};

			synchronize.Clicked += HandleSynchronize;

			Button reinitialize = new Button
			{
				Text = "Reinitialize",
				FontAttributes = FontAttributes.Bold,
				HorizontalOptions = LayoutOptions.Center
			};

			reinitialize.Clicked += HandleReinitialization;

			Button randomUpdate = new Button
			{
				Text = "Update some data!",
				FontAttributes = FontAttributes.Bold,
				HorizontalOptions = LayoutOptions.Center
			};

			randomUpdate.Clicked += HandleUpdateSomeData;

			lst = new ListView
			{
				//ItemsSource = people,
				ItemTemplate = new DataTemplate(() =>
				{
					var grid = new Grid();
					grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(0.1, GridUnitType.Star) });
					grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(0.6, GridUnitType.Star) });
					grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(0.3, GridUnitType.Star) });

					var idLabel = new Label { FontAttributes = FontAttributes.Bold };
					var nameLabel = new Label();
					var sizeLabel = new Label { HorizontalTextAlignment = TextAlignment.End };

					idLabel.SetBinding(Label.TextProperty, "Id");
					nameLabel.SetBinding(Label.TextProperty, "Name");
					sizeLabel.SetBinding(Label.TextProperty, "Size");
					idLabel.TextColor = Color.Blue;
					nameLabel.TextColor = Color.Black;
					sizeLabel.TextColor = Color.Black;

					grid.Children.Add(idLabel);
					grid.Children.Add(nameLabel, 1, 0);
					grid.Children.Add(sizeLabel, 2, 0);

					return new ViewCell
					{
						View = grid
					};
				})
			};

			Content = new StackLayout
			{
				Padding = new Thickness(0, 20, 0, 0),
				Children = {
					new Label {
						Text = "SQLite-sync.com sample",
						FontAttributes = FontAttributes.Bold,
						HorizontalOptions = LayoutOptions.Center
					},
					reinitialize,
					synchronize,
					randomUpdate,
					randomUpdate,
					lst
				}
			};

			BindData();
		}

		void BindData()
		{			
			try
			{
				var documents = new List<Document>();
				using (SqliteConnection conn = new SqliteConnection(GenerateConnectionString()))
				{
					using (SqliteCommand cmd = new SqliteCommand())
					{
						cmd.Connection = conn;
						conn.Open();
						SQLiteHelper sh = new SQLiteHelper(cmd);
						DataTable tables = sh.Select("select * from documents;");
						foreach (DataRow record in tables.Rows)
						{
							documents.Add(
								new Document(int.Parse(record["docId"].ToString()), record["docName"].ToString(), int.Parse(record["docSize"].ToString()))
							);
						}
					}
				}

				lst.ItemsSource = documents;
			}
			catch (Exception ex)
			{				
				DisplayAlert("SQLite-sync.com", "Reinitilize database!\r\n" + ex.Message, "OK");
			}
		}

		void HandleSynchronize(object sender, EventArgs ea)
		{
			string connString = GenerateConnectionString();
			string wsUrl = "http://demo.sqlite-sync.com/sync.asmx";
			SQLiteSyncCOMClient sync = new SQLiteSyncCOMClient(connString, wsUrl);
			sync.SendAndRecieveChanges("1");
			DisplayAlert("SQLite-sync.com", "Synchronization done!", "OK");
			BindData();
		}

		void HandleReinitialization(object sender, EventArgs ea)
		{
			string connString = GenerateConnectionString(); 
			string wsUrl = "http://demo.sqlite-sync.com/sync.asmx";
			SQLiteSyncCOMClient sync = new SQLiteSyncCOMClient(connString, wsUrl);
			sync.ReinitializeDatabase("1");
			DisplayAlert("SQLite-sync.com", "Reinitialization done!", "OK");
		}

		void HandleUpdateSomeData(object sender, EventArgs ea)
		{
			try
			{				
				using (SqliteConnection conn = new SqliteConnection(GenerateConnectionString()))
				{
					using (SqliteCommand cmd = new SqliteCommand())
					{
						cmd.Connection = conn;
						conn.Open();
						SQLiteHelper sh = new SQLiteHelper(cmd);
						sh.Execute("update documents set docSize=docSize*2;");
					}
				}
				DisplayAlert("SQLite-sync.com", "Now synchronize to see changes at server!", "OK");
				BindData();
			}
			catch (Exception ex)
			{
				DisplayAlert("SQLite-sync.com", "Reinitilize database!\r\n" + ex.Message, "OK");
			}
		}

		static string GenerateConnectionString()
		{
			string documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.Personal); // Documents folder
			string libraryPath = Path.Combine(documentsPath, "Library/"); // Library folderr
			if (!Directory.Exists(libraryPath))
			   Directory.CreateDirectory(libraryPath);

			string connString = "Data Source=" + libraryPath + "DemoT.db;";

			if (File.Exists(libraryPath + "DemoT.db;"))
				SqliteConnection.CreateFile(libraryPath + "DemoT.db;");

			return connString;
		}
	}
}
